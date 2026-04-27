package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import java.time.Instant
import java.util.UUID

data class PostResponse(
	val id: UUID,
	val title: String,
	val summary: String = "",
	val contentMarkdown: String,
	val thumbnailUrl: String? = null,
	val author: PostAuthorResponse,
	val collaborators: List<PostAuthorResponse> = emptyList(),
	val type: PostType,
	val status: PostStatus,
	val scheduledPublishAt: Instant? = null,
	val publishedAt: Instant? = null,
	val createdAt: Instant,
	val updatedAt: Instant,
)
