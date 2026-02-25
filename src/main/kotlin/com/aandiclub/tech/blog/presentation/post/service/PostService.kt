package com.aandiclub.tech.blog.presentation.post.service

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import java.util.UUID

interface PostService {
	suspend fun create(request: CreatePostRequest): PostResponse
	suspend fun get(postId: UUID): PostResponse
	suspend fun list(page: Int, size: Int, status: PostStatus?): PagedPostResponse
	suspend fun listDrafts(page: Int, size: Int): PagedPostResponse
	suspend fun patch(postId: UUID, request: PatchPostRequest): PostResponse
	suspend fun delete(postId: UUID)
}
