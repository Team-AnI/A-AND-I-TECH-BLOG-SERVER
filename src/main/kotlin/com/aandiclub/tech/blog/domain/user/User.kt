package com.aandiclub.tech.blog.domain.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("users")
data class User(
	@Id
	val id: String,
	val nickname: String,
	@Column("thumbnail_url")
	val thumbnailUrl: String? = null,
	@Column("created_at")
	val createdAt: Instant = Instant.now(),
	@Column("updated_at")
	val updatedAt: Instant = Instant.now(),
) {
	init {
		require(nickname.isNotBlank()) { "nickname must not be blank" }
		require(nickname.length <= 50) { "nickname must be less than or equal to 50 characters" }
	}
}
