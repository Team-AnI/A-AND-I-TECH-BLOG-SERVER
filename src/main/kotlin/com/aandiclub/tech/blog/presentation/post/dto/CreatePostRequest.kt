package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@JsonDeserialize(using = CreatePostRequestDeserializer::class)
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
	@get:JsonAlias("authorId")
	@field:JsonAlias("authorId")
	val author: PostAuthorRequest,
	val status: PostStatus = PostStatus.Published,
)
