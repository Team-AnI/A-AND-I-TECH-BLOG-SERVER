package com.aandiclub.tech.blog.common.auth

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Service
class JwtAuthTokenService(
	private val properties: JwtAuthProperties,
) : AuthTokenService {
	private val decoder: ReactiveJwtDecoder by lazy { createDecoder() }

	override suspend fun extractUserId(authorizationHeader: String?): String {
		val token = parseBearerToken(authorizationHeader)
		val jwt = try {
			decoder.decode(token).awaitSingle()
		} catch (_: JwtException) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token")
		}

		val userId = jwt.claims[properties.userIdClaim]?.toString()?.trim()
			?.takeIf { it.isNotBlank() }
			?: jwt.subject?.trim()?.takeIf { it.isNotBlank() }
			?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "user id claim not found")

		return userId
	}

	private fun parseBearerToken(authorizationHeader: String?): String {
		val raw = authorizationHeader?.trim()
		if (raw.isNullOrBlank()) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing Authorization header")
		}
		if (!raw.startsWith(BEARER_PREFIX, ignoreCase = true)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid Authorization header")
		}
		val token = raw.substringAfter(' ', "").trim()
		if (token.isBlank()) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token")
		}
		return token
	}

	private fun createDecoder(): ReactiveJwtDecoder {
		properties.sharedSecret?.trim()?.takeIf { it.isNotEmpty() }?.let { secret ->
			val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256)
			return NimbusReactiveJwtDecoder.withSecretKey(key)
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
		}

		properties.jwkSetUri?.trim()?.takeIf { it.isNotEmpty() }?.let { jwkSetUri ->
			return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
		}

		properties.issuerUri?.trim()?.takeIf { it.isNotEmpty() }?.let { issuerUri ->
			return ReactiveJwtDecoders.fromIssuerLocation(issuerUri)
		}

		throw ResponseStatusException(
			HttpStatus.UNAUTHORIZED,
			"jwt verifier is not configured",
		)
	}

	companion object {
		private const val BEARER_PREFIX = "Bearer "
		private const val HMAC_SHA256 = "HmacSHA256"
	}
}
