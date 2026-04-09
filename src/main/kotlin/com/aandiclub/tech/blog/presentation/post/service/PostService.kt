package com.aandiclub.tech.blog.presentation.post.service

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.dto.AddCollaboratorRequest
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import java.util.UUID

interface PostService {
	suspend fun create(request: CreatePostRequest): PostResponse
	suspend fun get(postId: UUID): PostResponse
	suspend fun list(page: Int, size: Int, status: PostStatus?, type: PostType?): PagedPostResponse
	suspend fun listMyPosts(page: Int, size: Int, requesterId: String, status: PostStatus?, type: PostType?): PagedPostResponse
	suspend fun listDrafts(page: Int, size: Int, type: PostType?): PagedPostResponse
	suspend fun listMyDrafts(page: Int, size: Int, requesterId: String, type: PostType?): PagedPostResponse
	suspend fun patch(postId: UUID, requesterId: String, request: PatchPostRequest): PostResponse
	suspend fun addCollaborator(postId: UUID, requesterId: String, request: AddCollaboratorRequest): PostResponse
	suspend fun delete(postId: UUID)
}
