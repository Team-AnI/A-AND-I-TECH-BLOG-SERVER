package com.aandiclub.tech.blog.infrastructure.post

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostStatus
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface PostRepository : CoroutineCrudRepository<Post, UUID> {
	suspend fun findByIdAndStatusNot(id: UUID, status: PostStatus): Post?

	fun findByStatus(status: PostStatus, pageable: Pageable): Flow<Post>

	suspend fun countByStatus(status: PostStatus): Long

	@Query(
		"""
		SELECT p.*
		FROM posts p
		WHERE p.status = 'Draft'
		  AND (
		    p.author_id = :userId
		    OR EXISTS (
		      SELECT 1
		      FROM post_collaborators pc
		      WHERE pc.post_id = p.id
		        AND pc.user_id = :userId
		    )
		  )
		ORDER BY p.created_at DESC
		LIMIT :limit OFFSET :offset
		""",
	)
	fun findDraftsByUser(userId: String, limit: Int, offset: Long): Flow<Post>

	@Query(
		"""
		SELECT COUNT(*)
		FROM posts p
		WHERE p.status = 'Draft'
		  AND (
		    p.author_id = :userId
		    OR EXISTS (
		      SELECT 1
		      FROM post_collaborators pc
		      WHERE pc.post_id = p.id
		        AND pc.user_id = :userId
		    )
		  )
		""",
	)
	suspend fun countDraftsByUser(userId: String): Long
}
