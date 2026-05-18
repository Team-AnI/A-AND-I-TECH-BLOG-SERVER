package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorCatalog
import com.aandiclub.tech.blog.common.api.v2.AiV2ProtocolException
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ResponseStatusException

class AiV2RequestContextResolverTest {
	private val authTokenService = mockk<AuthTokenService>()
	private val resolver = AiV2RequestContextResolver(authTokenService)

	@Test
	fun `public request should ignore optional Authenticate verifier failure`() = runBlocking {
		coEvery { authTokenService.extractUserId("Bearer broken-token") } throws
			ResponseStatusException(HttpStatus.UNAUTHORIZED, "jwt verifier is not configured")

		val context = resolver.resolvePublic(exchange(authenticate = "Bearer broken-token"))

		assertThat(context.authenticate).isEqualTo("Bearer broken-token")
		assertThat(context.requesterId).isNull()
	}

	@Test
	fun `authenticated request should reject Authenticate verifier failure`() {
		coEvery { authTokenService.extractUserId("Bearer broken-token") } throws
			ResponseStatusException(HttpStatus.UNAUTHORIZED, "jwt verifier is not configured")

		val exception = assertThrows(AiV2ProtocolException::class.java) {
			runBlocking {
				resolver.resolveAuthenticated(exchange(authenticate = "Bearer broken-token"))
			}
		}

		assertThat(exception.descriptor).isEqualTo(AiV2ErrorCatalog.invalidAuthenticate)
		assertThat(exception.message).isEqualTo("jwt verifier is not configured")
	}

	private fun exchange(authenticate: String? = null): MockServerWebExchange {
		val request = MockServerHttpRequest.get("/v2/blogs")
			.header("deviceOS", "IOS")
			.header("timestamp", "2026-04-09T12:00:00Z")
		authenticate?.let { request.header("Authenticate", it) }
		return MockServerWebExchange.from(request.build())
	}
}
