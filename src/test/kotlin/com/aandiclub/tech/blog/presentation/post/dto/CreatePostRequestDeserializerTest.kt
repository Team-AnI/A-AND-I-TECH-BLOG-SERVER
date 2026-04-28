package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

class CreatePostRequestDeserializerTest : StringSpec({
	"should deserialize legacy authorId string payload" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","authorId":"u-legacy-1","status":"Draft"}""",
			CreatePostRequest::class.java,
		)

		request.author.shouldBeInstanceOf<PostAuthorRequest>()
		request.author.id shouldBe "u-legacy-1"
		request.status.name shouldBe "Draft"
	}

	"should deserialize collaborators payload" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","author":{"id":"u-owner-1","nickname":"owner"},"collaborators":[{"id":"u-collab-1","nickname":"collab"}],"status":"Draft"}""",
			CreatePostRequest::class.java,
		)

		request.collaborators?.size shouldBe 1
		request.collaborators?.get(0)?.id shouldBe "u-collab-1"
	}

	"should deserialize payload with thumbnail and profile image url" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","thumbnailUrl":"https://cdn.example.com/posts/thumbnail-1.webp","author":{"id":"u-1001","nickname":"neo","profileImageUrl":"https://cdn.example.com/users/neo.webp"},"status":"Draft"}""",
			CreatePostRequest::class.java,
		)

		request.thumbnailUrl shouldBe "https://cdn.example.com/posts/thumbnail-1.webp"
		request.author.profileImageUrl shouldBe "https://cdn.example.com/users/neo.webp"
	}

	"should deserialize summary when provided" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","summary":"custom summary","contentMarkdown":"content","author":{"id":"u-1001","nickname":"neo"}}""",
			CreatePostRequest::class.java,
		)

		request.summary shouldBe "custom summary"
	}

	"should default type to Blog when omitted" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","author":{"id":"u-1001","nickname":"neo"}}""",
			CreatePostRequest::class.java,
		)

		request.type?.name shouldBe "Blog"
	}

	"should deserialize explicit type" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","author":{"id":"u-1001","nickname":"neo"},"type":"Lecture"}""",
			CreatePostRequest::class.java,
		)

		request.type?.name shouldBe "Lecture"
	}

	"should infer scheduled status when scheduledPublishAt is present" {
		val mapper = ObjectMapper().findAndRegisterModules()
		val request = mapper.readValue(
			"""{"title":"title","contentMarkdown":"content","author":{"id":"u-1001","nickname":"neo"},"scheduledPublishAt":"2026-05-01T12:00:00Z"}""",
			CreatePostRequest::class.java,
		)

		request.status shouldBe PostStatus.Scheduled
		request.scheduledPublishAt shouldBe Instant.parse("2026-05-01T12:00:00Z")
	}
})
