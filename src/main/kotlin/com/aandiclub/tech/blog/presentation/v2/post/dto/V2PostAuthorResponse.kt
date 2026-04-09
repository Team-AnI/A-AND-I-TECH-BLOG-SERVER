package com.aandiclub.tech.blog.presentation.v2.post.dto

data class V2PostAuthorResponse(
	val id: String,
	val nickname: String,
	val profileImageUrl: String? = null,
)
