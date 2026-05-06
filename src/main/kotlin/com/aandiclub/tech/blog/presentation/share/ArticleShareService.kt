package com.aandiclub.tech.blog.presentation.share

import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

interface ArticleShareService {
	suspend fun getArticleSharePage(postId: UUID): ArticleSharePageResponse
}

data class ArticleSharePageResponse(
	val status: HttpStatus,
	val html: String,
	val cacheControl: String,
	val lastModified: Instant? = null,
)
