package com.aandiclub.tech.blog.presentation.post.dto

import com.fasterxml.jackson.annotation.JsonAlias
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AddCollaboratorRequest(
	@field:Pattern(regexp = ".*\\S.*")
	@field:Size(max = 100)
	@param:JsonAlias("requesterId")
	@field:JsonAlias("requesterId")
	val ownerId: String? = null,
	@field:Valid
	@field:NotNull
	@param:JsonAlias("collaboratorId")
	@field:JsonAlias("collaboratorId")
	val collaborator: PostAuthorRequest,
)
