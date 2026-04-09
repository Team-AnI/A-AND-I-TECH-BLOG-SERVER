package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

@JsonDeserialize(using = PatchPostRequestDeserializer::class)
data class PatchPostRequest(
	@field:Size(min = 1, max = 200)
	val title: String? = null,
	@field:Size(max = 300)
	val summary: String? = null,
	val contentMarkdown: String? = null,
	val thumbnailUrl: String? = null,
	@field:Valid
	@param:JsonAlias("authorId")
	@get:JsonAlias("authorId")
	@field:JsonAlias("authorId")
	val author: PostAuthorRequest? = null,
	@field:Valid
	val collaborators: List<PostAuthorRequest>? = null,
	val type: PostType? = null,
	val status: PostStatus? = null,
)
