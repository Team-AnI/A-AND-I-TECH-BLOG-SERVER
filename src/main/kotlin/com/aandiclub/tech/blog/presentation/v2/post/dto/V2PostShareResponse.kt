package com.aandiclub.tech.blog.presentation.v2.post.dto

data class V2PostShareResponse(
	val shareUrl: String,
	val clientUrl: String,
	val title: String,
	val description: String,
	val imageUrl: String,
)
