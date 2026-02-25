package com.aandiclub.tech.blog.infrastructure.post

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface PostRepository : CoroutineCrudRepository<Post, UUID> {
	suspend fun findByIdAndStatusNot(id: UUID, status: PostStatus): Post?

	fun findByStatus(status: PostStatus, pageable: Pageable): Flow<Post>

	suspend fun countByStatus(status: PostStatus): Long
}
