package dev.lyzev.turnstile

object Config {
    val turnstileSecretKey: String
        get() = System.getenv("TURNSTILE_SECRET_KEY") ?: error("TURNSTILE_SECRET_KEY env var not set")

    val discordBotToken: String
        get() = System.getenv("DISCORD_BOT_TOKEN") ?: error("DISCORD_BOT_TOKEN env var not set")

    val guildId: ULong
        get() = System.getenv("GUILD_ID")?.toULongOrNull() ?: error("GUILD_ID env var not set")

    val roleId: ULong
        get() = System.getenv("ROLE_ID")?.toULongOrNull() ?: error("ROLE_ID env var not set")

    val webServerPort: Int
        get() = System.getenv("WEB_SERVER_PORT")?.toIntOrNull() ?: 8080

    val webServerHost: String
        get() = System.getenv("WEB_SERVER_HOST") ?: "0.0.0.0"
}