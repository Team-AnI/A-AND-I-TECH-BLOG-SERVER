package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class PatchPostRequest(
	@field:Size(min = 1, max = 200)
	val title: String? = null,
	val contentMarkdown: String? = null,
	val thumbnailUrl: String? = null,
	@field:Valid
	@field:JsonAlias("authorId")
	val author: PostAuthorRequest? = null,
	val status: PostStatus? = null,
)
