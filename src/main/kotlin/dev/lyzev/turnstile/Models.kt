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