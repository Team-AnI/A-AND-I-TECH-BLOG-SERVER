package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.service.PostService
import com.aandiclub.tech.blog.presentation.v2.post.V2PostController
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException

class AiV2ErrorHandlingTest : StringSpec({
	val postService = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val objectMapper = ObjectMapper().findAndRegisterModules()
	val validator = Validation.buildDefaultValidatorFactory().validator
	val client = WebTestClient.bindToController(V2PostController(postService, imageUploadService, headerResolver, objectMapper, validator))
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
			.jsonPath("$.error.message").isEqualTo("deviceOS 헤더가 필요합니다.")
			.jsonPath("$.error.value").isEqualTo("deviceOS 헤더가 필요합니다.")
			.jsonPath("$.error.alert").isEqualTo("앱 정보를 확인할 수 없어요. 앱을 다시 실행해 주세요.")
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
			.jsonPath("$.error.message").isEqualTo("게시글을 찾을 수 없습니다.")
			.jsonPath("$.error.value").isEqualTo("게시글을 찾을 수 없습니다.")
			.jsonPath("$.error.alert").isEqualTo("요청한 글을 찾을 수 없어요.")
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
			.jsonPath("$.error.message").isEqualTo("timestamp 헤더는 ISO-8601 형식이어야 합니다.")
			.jsonPath("$.error.alert").isEqualTo("요청 시간이 올바르지 않아요. 다시 시도해 주세요.")
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
			.jsonPath("$.error.message").isEqualTo("Authenticate 헤더가 필요합니다.")
			.jsonPath("$.error.alert").isEqualTo("로그인이 필요해요. 다시 로그인해 주세요.")
	}
})
