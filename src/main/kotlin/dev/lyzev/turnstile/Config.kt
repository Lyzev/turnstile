/*
 * This file is part of https://github.com/SchizoidDevelopment/piko.
 *
 * Copyright (c) 2025. Lyzev
 *
 * Piko is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at your option) any later version.
 *
 * Piko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Piko. If not, see https://www.gnu.org/licenses/agpl-3.0.en.html.
 */

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