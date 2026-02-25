package com.aandiclub.tech.blog.presentation.post.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@JsonDeserialize(using = PostAuthorRequestDeserializer::class)
data class PostAuthorRequest(
	@field:NotNull
	@field:Pattern(regexp = ".*\\S.*")
	@field:Size(max = 100)
	@param:JsonAlias("userId")
	@field:JsonAlias("userId")
	val id: String,
	@field:Size(max = 50)
	val nickname: String? = null,
	@param:JsonAlias("thumbnailUrl")
	@field:JsonAlias("thumbnailUrl")
	val profileImageUrl: String? = null,
) {
	init {
		require(nickname == null || nickname.isNotBlank()) { "nickname must not be blank" }
	}
}
