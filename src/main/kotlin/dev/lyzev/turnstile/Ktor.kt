package dev.lyzev.turnstile

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.http.encodeURLParameter
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

object WebServer {

    private val logger = LoggerFactory.getLogger(WebServer::class.java)
    private val criticalFileCache = mutableMapOf<String, Pair<ByteArray, ContentType>>()

    private var service: CloudflareService? = null
    private var server: NettyApplicationEngine? = null

    const val SUCCESS_PAGE = "/success"
    const val ERROR_PAGE = "/error"
    const val TURNSTILE_RESPONSE_PARAM = "cf-turnstile-response"
    const val AUTH_TOKEN_PARAM = "token"

    /**
     * Starts the web server.
     */
    fun start() {
        logger.info("Starting Turnstile web server on ${Config.webServerHost}:${Config.webServerPort}")

        try {
            service = CloudflareService(Config.turnstileSecretKey)

            preloadCriticalFiles()

            embeddedServer(Netty, port = Config.webServerPort, host = Config.webServerHost) {

                install(RateLimit) {
                    global {
                        rateLimiter(limit = 100, refillPeriod = 60.seconds)
                    }
                }

                install(CachingHeaders) {
                    options { _, _ ->
                        CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
                    }
                }

                routing {
                    get("/") { serveCachedFile("index.html") }
                    get("/success") { serveCachedFile("success.html") }
                    get("/error") { serveCachedFile("error.html") }
                    post("/") {
                        handleAuthentication()
                    }
                }
            }.start(wait = false)

            logger.info("Turnstile web server started successfully")
        } catch (e: Exception) {
            logger.error("Failed to start web server", e)
            throw e
        }
    }

    /**
     * Stops the web server and cleans up resources.
     */
    fun stop() {
        logger.info("Stopping Turnstile web server...")

        try {
            server?.stop(1000, 2000)
            service?.close()
            logger.info("Turnstile web server stopped successfully")
        } catch (e: Exception) {
            logger.error("Error stopping web server", e)
        }
    }

    private fun preloadCriticalFiles() {
        val criticalFiles = listOf(
            "index.html" to ContentType.Text.Html,
            "success.html" to ContentType.Text.Html,
            "error.html" to ContentType.Text.Html
        )

        criticalFiles.forEach { (filename, contentType) ->
            try {
                val resource = WebServer::class.java.classLoader.getResourceAsStream("static/$filename")
                if (resource != null) {
                    criticalFileCache[filename] = resource.readBytes() to contentType
                    logger.debug("Cached critical file: $filename")
                }
            } catch (e: Exception) {
                logger.error("Failed to cache critical file: $filename", e)
            }
        }
    }

    private suspend fun RoutingContext.serveCachedFile(filename: String) {
        val cached = criticalFileCache[filename]
        if (cached != null) {
            call.respondBytes(cached.first, cached.second)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    /**
     * Handles authentication requests from the web interface.
     */
    private suspend fun RoutingContext.handleAuthentication() {
        try {
            val params = call.receiveParameters()
            val response = params[TURNSTILE_RESPONSE_PARAM]
            val token = params[AUTH_TOKEN_PARAM]

            if (response.isNullOrBlank()) {
                val error = "Authentication request missing Turnstile token. Please try again."
                logger.warn(error)
                call.respondRedirect("$ERROR_PAGE?msg=${error.encodeURLParameter()}")
                return
            }

            if (token.isNullOrBlank()) {
                val error = "Authentication request missing authentication code. Please try again."
                logger.warn(error)
                call.respondRedirect("$ERROR_PAGE?msg=${error.encodeURLParameter()}")
                return
            }

            val remoteIp = getClientIp()

            val service = service
            if (service == null) {
                logger.error("Cloudflare service not initialized")
                call.respondRedirect("$ERROR_PAGE?msg=${"Internal server error. Please try again later.".encodeURLParameter()}")
                return
            }

            val turnstileResponse = service.verifyResponse(response, remoteIp)

            if (turnstileResponse.success) {
                if (!JWTService.verifyJWT(token)) {
                    logger.warn("Invalid authentication code: $token")
                    call.respondRedirect("$ERROR_PAGE?msg=${"Invalid authentication token. Please try again.".encodeURLParameter()}")
                    return
                }
                val payload = JWTService.decodePayload(token)
                if (payload.iat < (System.currentTimeMillis() - 5 * 60 * 1000)) {
                    call.respondRedirect(ERROR_PAGE)
                    return
                }
                UserAuthenticatedEvent(payload).fire()
                call.respondRedirect(SUCCESS_PAGE)
            } else {
                logger.warn("Turnstile verification failed, errors: ${turnstileResponse.errorCodes}")
                call.respondRedirect("$ERROR_PAGE?msg=${"Turnstile verification failed. Please try again.".encodeURLParameter()}")
            }
        } catch (e: Exception) {
            logger.error("Error during authentication", e)
            call.respondRedirect("$ERROR_PAGE?msg=${"Internal server error. Please try again later.".encodeURLParameter()}")
        }
    }

    private fun RoutingContext.getClientIp(): String {
        return call.request.headers["CF-Connecting-IP"]
            ?: call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.headers["X-Real-IP"]
            ?: call.request.origin.remoteHost
    }
}