package com.aandiclub.tech.blog.domain.post

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class PostTest : StringSpec({
	"title must be between 1 and 200 characters" {
		shouldThrow<IllegalArgumentException> {
			Post(
				title = "",
				contentMarkdown = "content",
				authorId = "u-1",
			)
		}

		shouldThrow<IllegalArgumentException> {
			Post(
				title = "a".repeat(201),
				contentMarkdown = "content",
				authorId = "u-2",
			)
		}
	}

	"blank content markdown is allowed for draft" {
		val post = Post(
			title = "draft title",
			contentMarkdown = " ",
			authorId = "u-3",
			status = PostStatus.Draft,
		)

		post.status shouldBe PostStatus.Draft
	}

	"content markdown must not be blank when published" {
		shouldThrow<IllegalArgumentException> {
			Post(
				title = "valid title",
				contentMarkdown = " ",
				authorId = "u-3",
				status = PostStatus.Published,
			)
		}
	}

	"scheduled post must include future schedule metadata" {
		shouldThrow<IllegalArgumentException> {
			Post(
				title = "valid title",
				contentMarkdown = "scheduled content",
				authorId = "u-6",
				status = PostStatus.Scheduled,
			)
		}
	}

	"scheduled post allows content when schedule exists" {
		val scheduleAt = Instant.parse("2026-04-30T12:00:00Z")
		val post = Post(
			title = "valid title",
			contentMarkdown = "scheduled content",
			authorId = "u-7",
			status = PostStatus.Scheduled,
			scheduledPublishAt = scheduleAt,
		)

		post.scheduledPublishAt shouldBe scheduleAt
	}

	"default status should be Draft" {
		val post = Post(
			title = "valid title",
			contentMarkdown = "content",
			authorId = "u-4",
		)

		post.status shouldBe PostStatus.Draft
	}

	"default type should be Blog" {
		val post = Post(
			title = "valid title",
			contentMarkdown = "content",
			authorId = "u-5",
		)

		post.type shouldBe PostType.Blog
	}
})
