package dev.lyzev.turnstile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

class CloudflareService(private val secretKey: String) {

    companion object {

        const val SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
        private val logger = LoggerFactory.getLogger(CloudflareService::class.java)
        private val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 10.seconds.inWholeMilliseconds
                connectTimeoutMillis = 5.seconds.inWholeMilliseconds
                socketTimeoutMillis = 10.seconds.inWholeMilliseconds
            }

            defaultRequest {
                header(HttpHeaders.UserAgent, "TurnstileBot/1.0.0")
            }
        }
    }

    suspend fun verifyResponse(response: String, remoteIp: String): TurnstileResponse {
        require(response.isNotBlank()) { "Turnstile response cannot be blank" }
        require(remoteIp.isNotBlank()) { "Remote IP cannot be blank" }

        val request = httpClient.post(SITEVERIFY_URL) {
            contentType(ContentType.Application.Json)
            setBody(TurnstileRequest(
                secret = secretKey,
                response = response,
                remoteip = remoteIp
            ))
        }

        when (request.status) {
            HttpStatusCode.OK -> {
                return request.body<TurnstileResponse>()
            }
            else -> {
                throw RuntimeException("Turnstile verification request failed with status: ${request.status}")
            }
        }
    }

    fun close() {
        try {
            httpClient.close()
            logger.debug("HTTP client closed successfully")
        } catch (e: Exception) {
            logger.warn("Error closing HTTP client", e)
        }
    }
}

object JWTService {

    // we can randomly generate a secret on each run since tokens are short-lived
    // this also means that tokens become invalid after a restart, which is fine
    // as they are only valid for a few minutes
    private val random = SecureRandom.getInstanceStrong()
    private val secret = ByteArray(32).apply { random.nextBytes(this) } // 256-bit secret
    private val mac = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(secret, "HmacSHA256"))
    }

    private val encodedHeader = buildJsonObject {
        put("alg", "HS256")
        put("typ", "JWT")
    }.toString().encodeBase64Url()

    private fun hmacSha256(data: String): String = mac.doFinal(data.encodeToByteArray()).encodeBase64Url()

    fun createJWT(payload: JWTPayload): String {
        val encodedPayload = Json.encodeToString(payload).encodeBase64Url()
        val signature = hmacSha256("$encodedHeader.$encodedPayload")
        return "$encodedHeader.$encodedPayload.$signature"
    }

    fun verifyJWT(token: String): Boolean {
        val parts = token.split('.')
        if (parts.size != 3) return false

        val (header, payload, signature) = parts
        val expectedSignature = hmacSha256("$header.$payload")
        return signature == expectedSignature
    }

    fun decodePayload(token: String): JWTPayload {
        val parts = token.split('.')
        if (parts.size != 3) throw IllegalArgumentException("Invalid JWT format")

        val payload = parts[1]
        val decodedString = payload.decodeBase64Url()

        return try {
            Json.decodeFromString(decodedString)
        } catch (_: Exception) {
            throw IllegalArgumentException("Failed to decode JWT payload")
        }
    }

    private fun ByteArray.encodeBase64Url() = Base64.UrlSafe.encode(this)
    private fun String.encodeBase64Url() = Base64.UrlSafe.encode(encodeToByteArray())
    private fun String.decodeBase64Url() = Base64.UrlSafe.decode(this).decodeToString()
}