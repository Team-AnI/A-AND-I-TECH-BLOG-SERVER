package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

class V2TypedPostQueryControllerTest : StringSpec({
	val service = mockk<PostService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val webTestClient = WebTestClient.bindToController(
		V2BlogQueryController(service, headerResolver),
		V2LectureQueryController(service, headerResolver),
	)
		.controllerAdvice(AiV2ExceptionHandler(AiV2ErrorMapper()))
		.build()
	val authenticate = "Bearer v2-typed-query-token"

	fun <S : WebTestClient.RequestHeadersSpec<S>> S.withV2Headers(): S =
		header("deviceOS", "IOS")
			.header("Authenticate", authenticate)
			.header("timestamp", "2026-04-09T12:00:00Z")

	"GET /v2/blogs should be public and fix type to Blog" {
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.list(0, 20, null, PostType.Blog) } returns
			PagedPostResponse(
				items = listOf(
					PostResponse(
						id = UUID.randomUUID(),
						title = "blog title",
						contentMarkdown = "blog content",
						author = PostAuthorResponse(
							id = "u-v2-blog",
							nickname = "blogger",
							profileImageUrl = null,
						),
						type = PostType.Blog,
						status = PostStatus.Published,
						createdAt = now,
						updatedAt = now,
					),
				),
				page = 0,
				size = 20,
				totalElements = 1,
				totalPages = 1,
			)

		webTestClient.get()
			.uri("/v2/blogs?page=0&size=20")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.items[0].type").isEqualTo("Blog")
	}

	"GET /v2/lectures should require v2 auth headers" {
		webTestClient.get()
			.uri("/v2/lectures?page=0&size=20")
			.exchange()
			.expectStatus().isBadRequest
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(90301)
	}

	"GET /v2/lectures/{id} should return not found for blog post id" {
		val postId = UUID.randomUUID()
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns "u-v2-lecture"
		coEvery { service.get(eq(postId)) } returns
			PostResponse(
				id = postId,
				title = "blog title",
				contentMarkdown = "blog content",
				author = PostAuthorResponse(
					id = "u-blog",
					nickname = "blogger",
					profileImageUrl = null,
				),
				type = PostType.Blog,
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.get()
			.uri("/v2/lectures/$postId")
			.withV2Headers()
			.exchange()
			.expectStatus().isNotFound
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
	}

	"GET /v2/lectures/me should use requester and fixed lecture type" {
		val requesterId = "u-v2-lecture-me"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.listMyPosts(0, 20, requesterId, null, PostType.Lecture) } returns
			PagedPostResponse(
				items = emptyList(),
				page = 0,
				size = 20,
				totalElements = 0,
				totalPages = 0,
			)

		webTestClient.get()
			.uri("/v2/lectures/me?page=0&size=20")
			.withV2Headers()
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.totalElements").isEqualTo(0)
	}
})
