package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreatePostRequest(
	@field:NotBlank
	@field:Size(max = 200)
	val title: String,
	@field:NotBlank
	val contentMarkdown: String,
	val thumbnailUrl: String? = null,
	@field:Valid
	@field:NotNull
	@param:JsonAlias("authorId")
	@field:JsonAlias("authorId")
	val author: PostAuthorRequest,
	val status: PostStatus = PostStatus.Published,
)
