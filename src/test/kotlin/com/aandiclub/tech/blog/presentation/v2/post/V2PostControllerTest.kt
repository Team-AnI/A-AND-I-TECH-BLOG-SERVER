package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class V2PostControllerTest : StringSpec({
	val service = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val objectMapper = ObjectMapper().findAndRegisterModules()
	val validator = Validation.buildDefaultValidatorFactory().validator
	val webTestClient = WebTestClient.bindToController(V2PostController(service, imageUploadService, headerResolver, objectMapper, validator))
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

	fun createPostResponse(
		postId: UUID,
		authorId: String,
		now: Instant,
		thumbnailUrl: String? = null,
		status: PostStatus = PostStatus.Published,
	): PostResponse =
		PostResponse(
			id = postId,
			title = "title",
			contentMarkdown = "content",
			thumbnailUrl = thumbnailUrl,
			author = PostAuthorResponse(
				id = authorId,
				nickname = "neo",
				profileImageUrl = null,
			),
			type = PostType.Blog,
			status = status,
			createdAt = now,
			updatedAt = now,
		)

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

	"POST /v2/posts should accept multipart post part with application/json" {
		val postId = UUID.randomUUID()
		val authorId = "u-v2-create-json"
		val requesterId = "u-v2-requester"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery {
			service.create(
				match {
					it.author.id == authorId &&
						it.status == PostStatus.Published &&
						it.type == PostType.Blog
				},
			)
		} returns createPostResponse(postId, authorId, now)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","author":{"id":"$authorId","nickname":"neo"},"type":"Blog","status":"Published"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

		webTestClient.post()
			.uri("/v2/posts")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.author.id").isEqualTo(authorId)
			.jsonPath("$.data.status").isEqualTo("Published")
	}

	"POST /v2/posts should accept multipart post part with application/octet-stream" {
		val postId = UUID.randomUUID()
		val authorId = "u-v2-create-octet"
		val requesterId = "u-v2-requester"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery {
			service.create(
				match {
					it.author.id == authorId &&
						it.title == "title" &&
						it.contentMarkdown == "content"
				},
			)
		} returns createPostResponse(postId, authorId, now)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			object : ByteArrayResource(
				"""{"title":"title","contentMarkdown":"content","author":{"id":"$authorId","nickname":"neo"},"type":"Blog","status":"Published"}"""
					.toByteArray(),
			) {
				override fun getFilename(): String = "post.json"
			},
		).contentType(MediaType.APPLICATION_OCTET_STREAM)

		webTestClient.post()
			.uri("/v2/posts")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.author.id").isEqualTo(authorId)
	}

	"POST /v2/posts should create draft when status is Draft" {
		val postId = UUID.randomUUID()
		val authorId = "u-v2-draft"
		val requesterId = "u-v2-requester"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery {
			service.create(
				match {
					it.author.id == authorId &&
						it.status == PostStatus.Draft
				},
			)
		} returns createPostResponse(postId, authorId, now, status = PostStatus.Draft)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","author":{"id":"$authorId","nickname":"neo"},"type":"Blog","status":"Draft"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

		webTestClient.post()
			.uri("/v2/posts")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.status").isEqualTo("Draft")
	}

	"POST /v2/posts multipart should upload thumbnail and keep existing flow" {
		val postId = UUID.randomUUID()
		val authorId = "u-v2-thumbnail"
		val requesterId = "u-v2-requester"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val uploadedThumbnailUrl = "https://cdn.example.com/posts/v2-uploaded-thumb.webp"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { imageUploadService.upload(any()) } returns
			ImageUploadResponse(
				url = uploadedThumbnailUrl,
				key = "posts/v2-uploaded-thumb.webp",
				contentType = "image/webp",
				size = 3,
			)
		coEvery {
			service.create(
				match {
					it.author.id == authorId &&
						it.thumbnailUrl == uploadedThumbnailUrl
				},
			)
		} returns createPostResponse(postId, authorId, now, thumbnailUrl = uploadedThumbnailUrl)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","thumbnailUrl":"https://cdn.example.com/posts/original-thumb.webp","author":{"id":"$authorId","nickname":"neo"},"type":"Blog","status":"Published"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		multipart.part(
			"thumbnail",
			object : ByteArrayResource(byteArrayOf(1, 2, 3)) {
				override fun getFilename(): String = "thumbnail.webp"
			},
		).contentType(MediaType.parseMediaType("image/webp"))

		webTestClient.post()
			.uri("/v2/posts")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.thumbnailUrl").isEqualTo(uploadedThumbnailUrl)
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

	"PATCH /v2/posts/{id} should map service 5xx response status exception to post update failed" {
		val postId = UUID.randomUUID()
		val requesterId = "u-v2-patch-5xx"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.patch(eq(postId), eq(requesterId), any()) } throws
			ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "db failed")

		webTestClient.patch()
			.uri("/v2/posts/$postId")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(PatchPostRequest(title = "updated"))
			.exchange()
			.expectStatus().is5xxServerError
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(64802)
			.jsonPath("$.error.value").isEqualTo("POST_UPDATE_FAILED")
	}

	"PATCH /v2/posts/{id} should map publish and unpublish 5xx failures to operation codes" {
		val publishPostId = UUID.randomUUID()
		val unpublishPostId = UUID.randomUUID()
		val requesterId = "u-v2-publish-5xx"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery {
			service.patch(eq(publishPostId), eq(requesterId), match { it.status == PostStatus.Published })
		} throws ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "publish failed")
		coEvery {
			service.patch(eq(unpublishPostId), eq(requesterId), match { it.status == PostStatus.Draft })
		} throws ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "unpublish failed")

		webTestClient.patch()
			.uri("/v2/posts/$publishPostId")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(PatchPostRequest(status = PostStatus.Published))
			.exchange()
			.expectStatus().is5xxServerError
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(64804)
			.jsonPath("$.error.value").isEqualTo("POST_PUBLISH_FAILED")

		webTestClient.patch()
			.uri("/v2/posts/$unpublishPostId")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(PatchPostRequest(status = PostStatus.Draft))
			.exchange()
			.expectStatus().is5xxServerError
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(64805)
			.jsonPath("$.error.value").isEqualTo("POST_UNPUBLISH_FAILED")
	}

	"PATCH /v2/posts/{id} should keep 4xx response status exception mapping" {
		val postId = UUID.randomUUID()
		val requesterId = "u-v2-patch-4xx"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.patch(eq(postId), eq(requesterId), any()) } throws
			ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner or collaborator can edit")

		webTestClient.patch()
			.uri("/v2/posts/$postId")
			.withAuthenticatedV2Headers()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(PatchPostRequest(title = "updated"))
			.exchange()
			.expectStatus().isForbidden
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(60201)
			.jsonPath("$.error.value").isEqualTo("POST_EDIT_FORBIDDEN")
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

	"GET /v2/posts/scheduled/me should return wrapped scheduled response" {
		val requesterId = "u-v2-scheduled"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.listMyScheduledPosts(0, 20, requesterId, null) } returns
			PagedPostResponse(
				items = listOf(
					PostResponse(
						id = UUID.randomUUID(),
						title = "scheduled title",
						contentMarkdown = "scheduled content",
						author = PostAuthorResponse(
							id = requesterId,
							nickname = "neo",
							profileImageUrl = null,
						),
						type = PostType.Blog,
						status = PostStatus.Scheduled,
						scheduledPublishAt = Instant.parse("2026-05-01T12:00:00Z"),
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
			.uri("/v2/posts/scheduled/me?page=0&size=20")
			.withAuthenticatedV2Headers()
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.items[0].status").isEqualTo("Scheduled")
			.jsonPath("$.data.items[0].scheduledPublishAt").isEqualTo("2026-05-01T12:00:00Z")
	}

	"legacy v2 /posts read endpoints should be marked deprecated" {
		val methods = V2PostController::class.java.declaredMethods.associateBy { it.name }

		listOf("get", "list", "listMyPosts", "listDrafts", "listMyDrafts", "listMyScheduledPosts").forEach { methodName ->
			val method = methods.getValue(methodName)
			method.isAnnotationPresent(Deprecated::class.java) shouldBe true
			method.getAnnotation(Operation::class.java)?.deprecated shouldBe true
		}
	}

	"DELETE /v2/posts/{id} should map service 5xx response status exception to post delete failed" {
		val postId = UUID.randomUUID()
		val requesterId = "u-v2-delete-5xx"
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns requesterId
		coEvery { service.delete(eq(postId)) } throws ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "db failed")

		webTestClient.delete()
			.uri("/v2/posts/$postId")
			.withAuthenticatedV2Headers()
			.exchange()
			.expectStatus().is5xxServerError
			.expectBody()
			.jsonPath("$.success").isEqualTo(false)
			.jsonPath("$.error.code").isEqualTo(64803)
			.jsonPath("$.error.value").isEqualTo("POST_DELETE_FAILED")
	}
})
