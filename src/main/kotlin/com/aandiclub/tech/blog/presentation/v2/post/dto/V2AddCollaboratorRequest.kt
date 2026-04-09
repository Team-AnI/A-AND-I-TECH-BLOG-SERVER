package com.aandiclub.tech.blog.presentation.v2.post.dto

import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class V2AddCollaboratorRequest(
	@field:Valid
	@field:NotNull
	val collaborator: PostAuthorRequest,
)
