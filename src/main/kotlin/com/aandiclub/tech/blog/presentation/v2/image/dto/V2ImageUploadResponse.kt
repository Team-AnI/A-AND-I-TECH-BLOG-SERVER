package com.aandiclub.tech.blog.presentation.v2.image.dto

data class V2ImageUploadResponse(
	val url: String,
	val key: String,
	val contentType: String,
	val size: Long,
)
