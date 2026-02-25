package com.aandiclub.tech.blog.presentation.post

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class PostControllerTest : StringSpec({
	val service = mockk<PostService>()
	val imageUploadService = mockk<ImageUploadService>()
	val webTestClient = WebTestClient.bindToController(PostController(service, imageUploadService)).build()

	"POST /v1/posts should return 201" {
		val postId = UUID.randomUUID()
		val authorId = "u-1001"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val thumbnailUrl = "https://cdn.example.com/posts/thumbnail-1.webp"
		val response = PostResponse(
			id = postId,
			title = "title",
			contentMarkdown = "content",
			thumbnailUrl = thumbnailUrl,
			author = PostAuthorResponse(
				id = authorId,
				nickname = "neo",
				profileImageUrl = "https://cdn.example.com/users/neo.webp",
			),
			status = PostStatus.Draft,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { service.create(any()) } returns response

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","thumbnailUrl":"$thumbnailUrl","author":{"id":"$authorId","nickname":"neo","profileImageUrl":"https://cdn.example.com/users/neo.webp"},"status":"Draft"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

		webTestClient.post()
			.uri("/v1/posts")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.thumbnailUrl").isEqualTo(thumbnailUrl)
			.jsonPath("$.data.author.id").isEqualTo(authorId)
			.jsonPath("$.data.author.nickname").isEqualTo("neo")
			.jsonPath("$.data.author.profileImageUrl").isEqualTo("https://cdn.example.com/users/neo.webp")
			.jsonPath("$.data.status").isEqualTo("Draft")
	}

	"POST /v1/posts should accept legacy authorId string" {
		val postId = UUID.randomUUID()
		val authorId = "u-legacy-1"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.create(match { it.author.id == authorId }) } returns
			PostResponse(
				id = postId,
				title = "title",
				contentMarkdown = "content",
				author = PostAuthorResponse(
					id = authorId,
					nickname = "unknown",
					profileImageUrl = null,
				),
				status = PostStatus.Draft,
				createdAt = now,
				updatedAt = now,
			)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","authorId":"$authorId","status":"Draft"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

		webTestClient.post()
			.uri("/v1/posts")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.author.id").isEqualTo(authorId)
			.jsonPath("$.data.status").isEqualTo("Draft")
	}

	"POST /v1/posts multipart should upload thumbnail and return 201" {
		val postId = UUID.randomUUID()
		val authorId = "u-1002"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val uploadedThumbnailUrl = "https://cdn.example.com/posts/uploaded-thumb.webp"
		coEvery { imageUploadService.upload(any()) } returns
			ImageUploadResponse(
				url = uploadedThumbnailUrl,
				key = "posts/uploaded-thumb.webp",
				contentType = "image/webp",
				size = 3,
			)
		coEvery {
			service.create(match { it.author.id == authorId && it.thumbnailUrl == uploadedThumbnailUrl })
		} returns
			PostResponse(
				id = postId,
				title = "title",
				contentMarkdown = "content",
				thumbnailUrl = uploadedThumbnailUrl,
				author = PostAuthorResponse(
					id = authorId,
					nickname = "neo",
					profileImageUrl = "https://cdn.example.com/users/neo.webp",
				),
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		val multipart = MultipartBodyBuilder()
		multipart.part(
			"post",
			"""{"title":"title","contentMarkdown":"content","author":{"id":"$authorId","nickname":"neo","profileImageUrl":"https://cdn.example.com/users/neo.webp"},"status":"Published"}""",
		).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		multipart.part(
			"thumbnail",
			object : ByteArrayResource(byteArrayOf(1, 2, 3)) {
				override fun getFilename(): String = "thumbnail.webp"
			},
		).contentType(MediaType.parseMediaType("image/webp"))

		webTestClient.post()
			.uri("/v1/posts")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.bodyValue(multipart.build())
			.exchange()
			.expectStatus().isCreated
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.id").isEqualTo(postId.toString())
			.jsonPath("$.data.thumbnailUrl").isEqualTo(uploadedThumbnailUrl)
			.jsonPath("$.data.author.id").isEqualTo(authorId)
			.jsonPath("$.data.status").isEqualTo("Published")
	}

	"GET /v1/posts/{id} should return 404 when not found" {
		coEvery { service.get(any()) } throws ResponseStatusException(HttpStatus.NOT_FOUND)

		webTestClient.get()
			.uri("/v1/posts/${UUID.randomUUID()}")
			.exchange()
			.expectStatus().isNotFound
	}

	"GET /v1/posts/{id} should include nested author" {
		val postId = UUID.randomUUID()
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.get(eq(postId)) } returns
			PostResponse(
				id = postId,
				title = "title",
				contentMarkdown = "content",
				thumbnailUrl = "https://cdn.example.com/posts/thumbnail-detail.webp",
				author = PostAuthorResponse(
					id = "u-2001",
					nickname = "상욱",
					profileImageUrl = "https://cdn.example.com/users/sangwook.webp",
				),
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.get()
			.uri("/v1/posts/$postId")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.author.id").isEqualTo("u-2001")
			.jsonPath("$.data.author.nickname").isEqualTo("상욱")
			.jsonPath("$.data.author.profileImageUrl").isEqualTo("https://cdn.example.com/users/sangwook.webp")
	}

	"GET /v1/posts should return paged response" {
		val authorId = "u-1003"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.list(0, 20, null) } returns
			PagedPostResponse(
				items = listOf(
					PostResponse(
						id = UUID.randomUUID(),
						title = "title",
						contentMarkdown = "content",
						thumbnailUrl = "https://cdn.example.com/posts/thumbnail-list.webp",
						author = PostAuthorResponse(
							id = authorId,
							nickname = "neo",
							profileImageUrl = "https://cdn.example.com/users/neo.webp",
						),
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
			.uri("/v1/posts?page=0&size=20")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.page").isEqualTo(0)
			.jsonPath("$.data.size").isEqualTo(20)
			.jsonPath("$.data.totalElements").isEqualTo(1)
			.jsonPath("$.data.totalPages").isEqualTo(1)
			.jsonPath("$.data.items[0].thumbnailUrl").isEqualTo("https://cdn.example.com/posts/thumbnail-list.webp")
			.jsonPath("$.data.items[0].author.id").isEqualTo(authorId)
			.jsonPath("$.data.items[0].status").isEqualTo("Published")
	}

	"GET /v1/posts/drafts should return draft paged response" {
		val authorId = "u-1004"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		coEvery { service.listDrafts(0, 20) } returns
			PagedPostResponse(
				items = listOf(
					PostResponse(
						id = UUID.randomUUID(),
						title = "draft title",
						contentMarkdown = "draft content",
						author = PostAuthorResponse(
							id = authorId,
							nickname = "neo",
							profileImageUrl = null,
						),
						status = PostStatus.Draft,
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
			.uri("/v1/posts/drafts?page=0&size=20")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.items[0].author.id").isEqualTo(authorId)
			.jsonPath("$.data.items[0].status").isEqualTo("Draft")
	}

	"PATCH /v1/posts/{id} should return 200" {
		val postId = UUID.randomUUID()
		val authorId = "u-1005"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val thumbnailUrl = "https://cdn.example.com/posts/thumbnail-updated.webp"
		coEvery { service.patch(eq(postId), any()) } returns
			PostResponse(
				id = postId,
				title = "updated",
				contentMarkdown = "updated-content",
				thumbnailUrl = thumbnailUrl,
				author = PostAuthorResponse(
					id = authorId,
					nickname = "neo",
					profileImageUrl = "https://cdn.example.com/users/neo.webp",
				),
				status = PostStatus.Published,
				createdAt = now,
				updatedAt = now,
			)

		webTestClient.patch()
			.uri("/v1/posts/$postId")
			.bodyValue(
				PatchPostRequest(
					title = "updated",
					contentMarkdown = "updated-content",
					thumbnailUrl = thumbnailUrl,
					status = PostStatus.Published,
				),
			)
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.title").isEqualTo("updated")
			.jsonPath("$.data.thumbnailUrl").isEqualTo(thumbnailUrl)
			.jsonPath("$.data.author.id").isEqualTo(authorId)
			.jsonPath("$.data.status").isEqualTo("Published")
	}

	"DELETE /v1/posts/{id} should return success envelope" {
		coEvery { service.delete(any()) } returns Unit

		webTestClient.delete()
			.uri("/v1/posts/${UUID.randomUUID()}")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.deleted").isEqualTo(true)
	}
})
