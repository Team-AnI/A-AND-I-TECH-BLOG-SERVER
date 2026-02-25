package com.aandiclub.tech.blog.domain.post

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("posts")
data class Post(
	@Id
	val id: UUID = UUID.randomUUID(),
	val title: String,
	@Column("content_markdown")
	val contentMarkdown: String,
	@Column("thumbnail_url")
	val thumbnailUrl: String? = null,
	@Column("author_id")
	val authorId: String,
	val status: PostStatus = PostStatus.Draft,
	@Column("created_at")
	val createdAt: Instant = Instant.now(),
	@Column("updated_at")
	val updatedAt: Instant = Instant.now(),
) {
	init {
		require(title.isNotBlank()) { "title must not be blank" }
		require(title.length <= 200) { "title must be less than or equal to 200 characters" }
		require(contentMarkdown.isNotBlank()) { "contentMarkdown must not be blank" }
	}
}
