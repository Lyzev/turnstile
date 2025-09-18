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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for Turnstile verification API.
 */
@Serializable
data class TurnstileRequest(
    val secret: String,
    val response: String,
    val remoteip: String
)

/**
 * Response model from Turnstile verification API.
 */
@Serializable
data class TurnstileResponse(
    val success: Boolean,
    @SerialName("challenge_ts")
    val challengeTs: String? = null,
    val hostname: String? = null,
    @SerialName("error-codes")
    val errorCodes: List<String> = emptyList(),
    val action: String? = null,
    val cdata: String? = null,
    val metadata: TurnstileMetadata? = null
)

/**
 * Metadata information from Turnstile response.
 */
@Serializable
data class TurnstileMetadata(
    @SerialName("ephemeral_id")
    val ephemeralId: String? = null
)

/**
 * JWT Payload data class.
 */
@Serializable
data class JWTPayload(
    val sub: ULong,
    val iat: Long
)