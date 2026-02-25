package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import java.time.Instant
import java.util.UUID

data class PostResponse(
	val id: UUID,
	val title: String,
	val contentMarkdown: String,
	val thumbnailUrl: String? = null,
	val author: PostAuthorResponse,
	val status: PostStatus,
	val createdAt: Instant,
	val updatedAt: Instant,
)
