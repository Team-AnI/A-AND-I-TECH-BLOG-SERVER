package com.aandiclub.tech.blog.presentation.share

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostShareResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class ArticleShareServiceTest : StringSpec({
	val postService = mockk<PostService>()
	val properties = SharePageProperties(
		publicBaseUrl = "https://tech.aandiclub.com",
		defaultOgImageUrl = "https://tech.aandiclub.com/images/default-og.png",
		defaultDescription = "A&I 기술 블로그",
		cache = ShareCacheProperties(
			maxAgeSeconds = 60,
			sharedMaxAgeSeconds = 300,
			staleWhileRevalidateSeconds = 600,
		),
	)
	val service = ArticleShareServiceImpl(postService, properties)

	"published blog should return dynamic og html" {
		val postId = UUID.randomUUID()
		val updatedAt = Instant.parse("2026-05-06T00:00:00Z")
		coEvery { postService.get(postId) } returns
			PostResponse(
				id = postId,
				title = "A&I Dynamic OG",
				summary = "요약입니다.",
				contentMarkdown = "# heading\n본문입니다.",
				thumbnailUrl = "https://cdn.aandiclub.com/posts/thumb.png",
				author = PostAuthorResponse(id = "user-1", nickname = "neo", profileImageUrl = null),
				type = PostType.Blog,
				status = PostStatus.Published,
				publishedAt = updatedAt,
				createdAt = updatedAt,
				updatedAt = updatedAt,
				share = PostShareResponse(
					shareUrl = "https://tech.aandiclub.com/share/articles/$postId",
					clientUrl = "https://tech.aandiclub.com/articles/$postId",
					title = "A&I Dynamic OG",
					description = "요약입니다.",
					imageUrl = "https://cdn.aandiclub.com/posts/thumb.png",
				),
			)

		val response = service.getArticleSharePage(postId)

		response.status shouldBe HttpStatus.OK
		response.cacheControl shouldBe "public, max-age=60, s-maxage=300, stale-while-revalidate=600"
		response.lastModified shouldBe updatedAt
		response.html.shouldContain("""<meta property="og:title" content="A&amp;I Dynamic OG" />""")
		response.html.shouldContain("""<meta property="og:description" content="요약입니다." />""")
		response.html.shouldContain("""<meta property="og:image" content="https://cdn.aandiclub.com/posts/thumb.png" />""")
		response.html.shouldContain("""<meta property="og:url" content="https://tech.aandiclub.com/share/articles/$postId" />""")
		response.html.shouldContain("""<meta http-equiv="refresh" content="0; url=https://tech.aandiclub.com/articles/$postId" />""")
	}

	"draft or missing article should return 404 default og html" {
		val postId = UUID.randomUUID()
		coEvery { postService.get(postId) } throws ResponseStatusException(HttpStatus.NOT_FOUND)

		val response = service.getArticleSharePage(postId)

		response.status shouldBe HttpStatus.NOT_FOUND
		response.cacheControl shouldBe "no-store, max-age=0"
		response.html.shouldContain("""<meta property="og:title" content="게시글을 찾을 수 없습니다" />""")
		response.html.shouldContain("""<meta property="og:image" content="https://tech.aandiclub.com/images/default-og.png" />""")
	}

	"non-blog post should return 404 default og html" {
		val postId = UUID.randomUUID()
		coEvery { postService.get(postId) } returns
			PostResponse(
				id = postId,
				title = "Lecture",
				contentMarkdown = "content",
				author = PostAuthorResponse(id = "user-3", nickname = "lecturer", profileImageUrl = null),
				type = PostType.Lecture,
				status = PostStatus.Published,
				createdAt = Instant.parse("2026-05-06T00:00:00Z"),
				updatedAt = Instant.parse("2026-05-06T00:00:00Z"),
				share = null,
			)

		val response = service.getArticleSharePage(postId)

		response.status shouldBe HttpStatus.NOT_FOUND
	}
})
