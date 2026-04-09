package com.aandiclub.tech.blog.presentation.v2.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import java.time.Instant
import java.util.UUID

data class V2PostResponse(
	val id: UUID,
	val title: String,
	val summary: String = "",
	val contentMarkdown: String,
	val thumbnailUrl: String? = null,
	val author: V2PostAuthorResponse,
	val collaborators: List<V2PostAuthorResponse> = emptyList(),
	val type: PostType,
	val status: PostStatus,
	val createdAt: Instant,
	val updatedAt: Instant,
)
