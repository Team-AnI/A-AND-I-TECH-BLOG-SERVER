package com.aandiclub.tech.blog.presentation.share

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

class PostShareMetadataFactoryTest : StringSpec({
	val factory = PostShareMetadataFactory(
		SharePageProperties(
			publicBaseUrl = "https://tech.aandiclub.com",
			defaultOgImageUrl = "https://tech.aandiclub.com/images/default-og.png",
			defaultDescription = "A&I 기술 블로그",
		),
	)

	"published blog should build share metadata" {
		val postId = UUID.randomUUID()
		val share = factory.create(
			Post(
				id = postId,
				title = "A&I Dynamic OG",
				summary = "요약입니다.",
				contentMarkdown = "본문",
				thumbnailUrl = "https://cdn.aandiclub.com/posts/thumb.png",
				authorId = "user-1",
				type = PostType.Blog,
				status = PostStatus.Published,
			),
		)

		share!!.shareUrl shouldBe "https://tech.aandiclub.com/share/articles/$postId"
		share.clientUrl shouldBe "https://tech.aandiclub.com/articles/$postId"
		share.title shouldBe "A&I Dynamic OG"
		share.description shouldBe "요약입니다."
		share.imageUrl shouldBe "https://cdn.aandiclub.com/posts/thumb.png"
	}

	"missing summary or thumbnail should fallback to excerpt and default image" {
		val share = factory.create(
			Post(
				id = UUID.randomUUID(),
				title = "Fallback Post",
				summary = "",
				contentMarkdown = "## 소개\n이 글은 summary 없이도 OG description 이 생성되어야 합니다.",
				thumbnailUrl = null,
				authorId = "user-2",
				type = PostType.Blog,
				status = PostStatus.Published,
			),
		)

		share!!.description shouldBe "소개 이 글은 summary 없이도 OG description 이 생성되어야 합니다."
		share.imageUrl shouldBe "https://tech.aandiclub.com/images/default-og.png"
	}

	"draft or lecture should not expose share metadata" {
		factory.create(
			Post(
				id = UUID.randomUUID(),
				title = "Draft",
				contentMarkdown = "content",
				authorId = "user-3",
				type = PostType.Blog,
				status = PostStatus.Draft,
			),
		).shouldBeNull()

		factory.create(
			Post(
				id = UUID.randomUUID(),
				title = "Lecture",
				contentMarkdown = "content",
				authorId = "user-4",
				type = PostType.Lecture,
				status = PostStatus.Published,
			),
		).shouldBeNull()
	}
})
