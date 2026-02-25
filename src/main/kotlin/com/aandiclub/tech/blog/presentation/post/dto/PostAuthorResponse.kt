package com.aandiclub.tech.blog.presentation.post.dto

data class PostAuthorResponse(
	val id: String,
	val nickname: String,
	val profileImageUrl: String? = null,
)
