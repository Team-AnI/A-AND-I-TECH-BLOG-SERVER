package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.service.PostService
import com.aandiclub.tech.blog.presentation.v2.post.V2PostController
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException

class AiV2ErrorHandlingTest : StringSpec({
	val postService = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val client = WebTestClient.bindToController(V2PostController(postService, imageUploadService, headerResolver))
		.controllerAdvice(AiV2ExceptionHandler(AiV2ErrorMapper()))
		.build()

	"v2 should reject request without required headers" {
		client.get()
			.uri("/v2/posts/${java.util.UUID.randomUUID()}")
			.exchange()
			.expectStatus().isBadRequest
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(90301)
			.jsonPath("$.error.message").isEqualTo("deviceOS header is required")
			.jsonPath("$.error.value").isEqualTo("deviceOS header is required")
			.jsonPath("$.timestamp").exists()
	}

	"v2 should map not found into A&I error envelope" {
		coEvery { postService.get(any()) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "post not found")

		client.get()
			.uri("/v2/posts/${java.util.UUID.randomUUID()}")
			.header("deviceOS", "IOS")
			.header("timestamp", "2026-04-09T12:00:00Z")
			.exchange()
			.expectStatus().isNotFound
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(60501)
			.jsonPath("$.error.message").isEqualTo("post not found")
			.jsonPath("$.error.value").isEqualTo("post not found")
	}

	"v2 should reject invalid timestamp header" {
		client.get()
			.uri("/v2/posts/${java.util.UUID.randomUUID()}")
			.header("deviceOS", "AOS")
			.header("timestamp", "not-an-instant")
			.exchange()
			.expectStatus().isBadRequest
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(90303)
			.jsonPath("$.error.value").isEqualTo("not-an-instant")
	}

	"v2 protected endpoint should reject request without Authenticate header" {
		client.get()
			.uri("/v2/posts/me")
			.header("deviceOS", "IOS")
			.header("timestamp", "2026-04-09T12:00:00Z")
			.exchange()
			.expectStatus().isUnauthorized
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(90101)
			.jsonPath("$.error.message").isEqualTo("Authenticate header is required")
	}
})
