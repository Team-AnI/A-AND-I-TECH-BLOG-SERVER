package com.aandiclub.tech.blog.presentation.post.dto

data class PostShareResponse(
	val shareUrl: String,
	val clientUrl: String,
	val title: String,
	val description: String,
	val imageUrl: String,
)
