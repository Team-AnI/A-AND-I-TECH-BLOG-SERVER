package com.aandiclub.tech.blog.infrastructure.post

import com.aandiclub.tech.blog.domain.post.PostCollaborator
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface PostCollaboratorRepository : CoroutineCrudRepository<PostCollaborator, UUID> {
	fun findByPostId(postId: UUID): Flow<PostCollaborator>

	fun findByPostIdIn(postIds: Collection<UUID>): Flow<PostCollaborator>

	suspend fun existsByPostIdAndUserId(postId: UUID, userId: String): Boolean
}
