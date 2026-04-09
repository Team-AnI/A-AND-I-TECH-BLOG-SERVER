package com.aandiclub.tech.blog.presentation.v2.post.dto

data class V2PagedPostResponse(
	val items: List<V2PostResponse>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)
