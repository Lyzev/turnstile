package dev.lyzev.turnstile

import dev.lyzev.api.event.Event

class UserAuthenticatedEvent(payload: JWTPayload) : Event {
    val userId = payload.sub
    val issuedAt = payload.iat
}