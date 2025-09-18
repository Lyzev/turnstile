package dev.lyzev.turnstile

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("dev.lyzev.turnstile.Main")

fun main() {
    logger.info("Starting Turnstile application...")

    try {
        // Setup graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown hook triggered, stopping services...")
            WebServer.stop()
        })

        // Start services
        runBlocking {
            try {
                // Start web server in background
                val webServerJob = launch {
                    WebServer.start()
                }

                // Start Discord bot (this blocks)
                DiscordBot.start()

                // Wait for web server job to complete (it shouldn't under normal circumstances)
                webServerJob.join()

            } catch (e: CancellationException) {
                logger.info("Application cancelled")
                throw e
            } catch (e: Exception) {
                logger.error("Error during application runtime", e)
                throw e
            }
        }

    } catch (e: IllegalStateException) {
        logger.error("Configuration error: ${e.message}")
        logger.error("Please ensure you have a valid config.json file in the project root directory.")
        logger.error("You can use config.json.example as a template and fill in your actual values:")
        logger.error("- turnstileSecretKey: Your Cloudflare Turnstile secret key")
        logger.error("- turnstileSiteKey: Your Cloudflare Turnstile site key")
        logger.error("- discordBotToken: Your Discord bot token")
        exitProcess(1)
    } catch (e: Exception) {
        logger.error("Failed to start Turnstile application", e)
        exitProcess(1)
    }
}