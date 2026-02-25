package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.error.GlobalExceptionHandler
import com.aandiclub.tech.blog.common.filter.CorrelationIdFilter
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.PostController
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class ErrorHandlingTest : StringSpec({
	val postService = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val client = WebTestClient.bindToController(PostController(postService, imageUploadService))
		.controllerAdvice(GlobalExceptionHandler())
		.build()

	"response status exception should follow error response format with request traceId" {
		coEvery { postService.get(any()) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "post not found")
		val postId = UUID.randomUUID()

		client.get()
			.uri("/v1/posts/$postId")
			.header(CorrelationIdFilter.HEADER_NAME, "trace-123")
			.exchange()
			.expectStatus().isNotFound
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo("NOT_FOUND")
			.jsonPath("$.error.message").isEqualTo("post not found")
			.jsonPath("$.timestamp").exists()
	}

	"validation error should return standardized body" {
		val userId = "u-validation-1"
		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"","contentMarkdown":"content","author":{"id":"$userId","nickname":"neo","profileImageUrl":"https://cdn.example.com/users/neo.webp"},"status":"${PostStatus.Draft.name}"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

		client.post()
			.uri("/v1/posts")
			.header(CorrelationIdFilter.HEADER_NAME, "trace-validation-1")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
				.exchange()
				.expectStatus().isBadRequest
				.expectBody()
				.jsonPath("$.success").isEqualTo(false)
				.jsonPath("$.error.code").isEqualTo("VALIDATION_FAILED")
				.jsonPath("$.timestamp").exists()
	}
})
