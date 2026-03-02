package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@JsonDeserialize(using = PatchPostRequestDeserializer::class)
data class PatchPostRequest(
	@field:Size(min = 1, max = 200)
	val title: String? = null,
	val contentMarkdown: String? = null,
	val thumbnailUrl: String? = null,
	@field:Valid
	@param:JsonAlias("authorId")
	@get:JsonAlias("authorId")
	@field:JsonAlias("authorId")
	val author: PostAuthorRequest? = null,
	@field:Valid
	val collaborators: List<PostAuthorRequest>? = null,
	val status: PostStatus? = null,
)
