package dev.lyzev.turnstile

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.lyzev.api.event.EventListener
import dev.lyzev.api.event.on
import dev.lyzev.turnstile.Config.guildId
import dev.lyzev.turnstile.Config.roleId
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object DiscordBot : EventListener {

    private val logger = LoggerFactory.getLogger(DiscordBot::class.java)

    const val AUTHENTICATE_BUTTON_ID = "authenticate"
    const val WHY_BUTTON_ID = "why"
    const val VERIFICATION_URL_BASE = "https://authenticate.lyzev.dev?token="

    suspend fun start() {
        logger.info("Starting Discord bot...")

        val guild = Snowflake(guildId)
        val role = Snowflake(roleId)

        try {
            val bot = Kord(Config.discordBotToken)

            on<UserAuthenticatedEvent> { event ->
                val userId = Snowflake(event.userId)
                @Suppress("RunBlockingInSuspendFunction")
                runBlocking {
                    val member = bot.getGuild(guild).getMember(userId)
                    if (role in member.roleIds) {
                        logger.info("User $userId already has the role, skipping assignment")
                        return@runBlocking
                    }
                    member.addRole(role, "User completed authentication")
                    logger.info("Assigned role to user: $userId")
                }
            }

            bot.createGuildChatInputCommand(guild, "auth", "Set up authentication for the server.") {
                defaultMemberPermissions = Permissions(Permission.Administrator)
            }
            bot.on<GuildChatInputCommandInteractionCreateEvent> {
                interaction.channel.createMessage {
                    embed {
                        title = ":robot: Authentication Required"
                        color = Color(0xfe5e00)
                        description = "To access the server, please complete the authentication process by clicking the button below.\n\nThis helps us ensure that you are a human and not a bot."
                        footer {
                            text = "Powered by Cloudflare Turnstile"
                        }
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Secondary, "disabled") {
                            label = "ONLY authenticate on https://authenticate.lyzev.dev"
                            disabled = true
                        }
                    }
                    actionRow {
                        interactionButton(ButtonStyle.Primary, AUTHENTICATE_BUTTON_ID) {
                            label = "Authenticate"
                            emoji(ReactionEmoji.Unicode("\u270D\uFE0F"))
                        }
                        interactionButton(ButtonStyle.Secondary, WHY_BUTTON_ID) {
                            label = "Why?"
                        }
                    }
                }
                interaction.respondEphemeral {
                    content = "Done! Please check the channel for the authentication message."
                }
            }

            bot.on<InteractionCreateEvent> {
                val interaction = interaction
                if (interaction is ButtonInteraction) {
                    when (interaction.componentId) {
                        AUTHENTICATE_BUTTON_ID -> {
                            if (role in interaction.user.asMember(guild).roleIds) {
                                interaction.respondEphemeral {
                                    content = ":ballot_box_with_check: You are already authenticated and have access to the server."
                                }
                                return@on
                            }
                            try {
                                val userId = interaction.user.id.value
                                val payload = JWTPayload(userId, System.currentTimeMillis())
                                val jwt = JWTService.createJWT(payload)

                                interaction.respondEphemeral {
                                    embed {
                                        title = ":link: **Cloudflare Authentication**"
                                        description = "Click the link below to complete your authentication:\n**$VERIFICATION_URL_BASE$jwt**"
                                        color = Color(0xfe5e00)
                                        footer {
                                            text = "Expires in 5 minutes for security"
                                        }
                                    }
                                }

                                logger.info("Generated authentication JWT for user: $userId")
                            } catch (e: Exception) {
                                logger.error("Failed to handle authentication button interaction", e)
                                try {
                                    interaction.respondEphemeral {
                                        content = ":x: An error occurred. Please try again later."
                                    }
                                } catch (responseError: Exception) {
                                    logger.error("Failed to send error response", responseError)
                                }
                            }
                        }
                        WHY_BUTTON_ID -> {
                            interaction.respondEphemeral {
                                content = """
                                    :shield: **This server uses [Cloudflare Turnstile](<https://www.cloudflare.com/application-services/products/turnstile/>) to prevent bot access and keep our community safe.**
                                    
                                    :lock: **Privacy First:**  
                                    This bot **cannot** see your personal information and does **not** store any data. You can verify this by checking the [source code on GitHub](<https://github.com/lyzev/turnstile>).
                                    
                                    :question: **Why Turnstile?**  
                                    Cloudflare Turnstile is a modern CAPTCHA alternative that is user-friendly and respects your privacy. It helps us keep the server secure, without annoying, traditional CAPTCHAs.
                                    
                                    :mag_right: **Learn more:**  
                                    - [How Turnstile works](<https://developers.cloudflare.com/turnstile/>)  
                                    - [Turnstile Privacy Policy](<https://www.cloudflare.com/turnstile-privacy-policy/>)
                                """.trimIndent()
                            }
                        }
                    }
                }
            }

            bot.login {
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }

            logger.info("Discord bot started successfully")
        } catch (e: Exception) {
            logger.error("Failed to start Discord bot", e)
            throw e
        }
    }

    override val shouldHandleEvents = true
}