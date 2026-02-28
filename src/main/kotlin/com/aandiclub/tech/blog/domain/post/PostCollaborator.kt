package com.aandiclub.tech.blog.domain.post

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("post_collaborators")
data class PostCollaborator(
	@Id
	val id: UUID = UUID.randomUUID(),
	@Column("post_id")
	val postId: UUID,
	@Column("user_id")
	val userId: String,
	@Column("created_at")
	val createdAt: Instant = Instant.now(),
)
