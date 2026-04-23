package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

class V2PostControllerTest : StringSpec({
	val service = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val webTestClient = WebTestClient.bindToController(V2PostController(service, imageUploadService, headerResolver))
		.controllerAdvice(AiV2ExceptionHandler(AiV2ErrorMapper()))
		.build()
	val authenticate = "Bearer v2-post-token"

	fun <S : WebTestClient.RequestHeadersSpec<S>> S.withV2Headers(): S =
		header("deviceOS", "IOS")
			.header("timestamp", "2026-04-09T12:00:00Z")

	fun <S : WebTestClient.RequestHeadersSpec<S>> S.withAuthenticatedV2Headers(): S =
		header("deviceOS", "IOS")
			.header("Authenticate", authenticate)
			.header("timestamp", "2026-04-09T12:00:00Z")

	"GET /v2/posts should return wrapped v2 paged response" {
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.list(0, 20, null, null) } returns
			PagedPostResponse(
				items = listOf(
					PostResponse(
						id = UUID.randomUUID(),
						title = "title",
						summary = "summary",
						contentMarkdown = "content",
						thumbnailUrl = "https://cdn.example.com/posts/v2.webp",
						author = PostAuthorResponse(
							id = "u-v2-list",
							nickname = "neo",
							profileImageUrl = "https://cdn.example.com/users/neo.webp",
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
			.uri("/v2/posts?page=0&size=20")
			.withV2Headers()
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.page").isEqualTo(0)
			.jsonPath("$.data.items[0].author.id").isEqualTo("u-v2-list")
			.jsonPath("$.data.items[0].thumbnailUrl").isEqualTo("https://cdn.example.com/posts/v2.webp")
			.jsonPath("$.data.items[0].status").isEqualTo("Published")
	}

	"GET /v2/posts/{id} should allow public access without Authenticate header" {
		val postId = UUID.randomUUID()
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.get(eq(postId)) } returns
			PostResponse(
				id = postId,
				title = "public-post",
				contentMarkdown = "public-content",
				author = PostAuthorResponse(
					id = "public-author",
					nickname = "neo",
					profileImageUrl = null,
				),
				type = PostType.Blog,
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.get()
			.uri("/v2/posts/$postId")
			.withV2Headers()
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.author.id").isEqualTo("public-author")
	}

	"PATCH /v2/posts/{id} should use Authenticate header requester and return v2 envelope" {
		val postId = UUID.randomUUID()
		val requesterId = "u-v2-patch"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.patch(eq(postId), eq(requesterId), any()) } returns
			PostResponse(
				id = postId,
				title = "updated",
				contentMarkdown = "updated-content",
				author = PostAuthorResponse(
					id = requesterId,
					nickname = "neo",
					profileImageUrl = null,
				),
				type = PostType.Lecture,
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.patch()
			.uri("/v2/posts/$postId")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(
				PatchPostRequest(
					title = "updated",
					contentMarkdown = "updated-content",
					type = PostType.Lecture,
					status = PostStatus.Published,
				),
			)
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.author.id").isEqualTo(requesterId)
			.jsonPath("$.data.type").isEqualTo("Lecture")
	}

	"POST /v2/posts/{id}/collaborators should accept v2 request without ownerId" {
		val postId = UUID.randomUUID()
		val requesterId = "u-v2-owner"
		val collaboratorId = "u-v2-collab"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.addCollaborator(eq(postId), eq(requesterId), match { it.ownerId == null && it.collaborator.id == collaboratorId }) } returns
			PostResponse(
				id = postId,
				title = "title",
				contentMarkdown = "content",
				author = PostAuthorResponse(
					id = requesterId,
					nickname = "owner",
					profileImageUrl = null,
				),
				collaborators = listOf(
					PostAuthorResponse(
						id = collaboratorId,
						nickname = "collab",
						profileImageUrl = null,
					),
				),
				type = PostType.Blog,
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.post()
			.uri("/v2/posts/$postId/collaborators")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(
				"""
				{
				  "collaborator": {
				    "id": "$collaboratorId",
				    "nickname": "collab"
				  }
				}
				""".trimIndent(),
			)
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.collaborators[0].id").isEqualTo(collaboratorId)
	}

	"legacy v2 /posts read endpoints should be marked deprecated" {
		val methods = V2PostController::class.java.declaredMethods.associateBy { it.name }

		listOf("get", "list", "listMyPosts", "listDrafts", "listMyDrafts").forEach { methodName ->
			val method = methods.getValue(methodName)
			method.isAnnotationPresent(Deprecated::class.java) shouldBe true
			method.getAnnotation(Operation::class.java)?.deprecated shouldBe true
		}
	}
})
