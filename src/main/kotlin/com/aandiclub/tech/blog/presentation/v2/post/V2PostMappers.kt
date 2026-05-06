package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PostShareResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PagedPostResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PostAuthorResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PostResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PostShareResponse

internal fun PostResponse.toV2(): V2PostResponse =
	V2PostResponse(
		id = id,
		title = title,
		summary = summary,
		contentMarkdown = contentMarkdown,
		thumbnailUrl = thumbnailUrl,
		author = author.toV2(),
		collaborators = collaborators.map { it.toV2() },
		type = type,
		status = status,
		scheduledPublishAt = scheduledPublishAt,
		publishedAt = publishedAt,
		createdAt = createdAt,
		updatedAt = updatedAt,
		share = share?.toV2(),
	)

internal fun PagedPostResponse.toV2(): V2PagedPostResponse =
	V2PagedPostResponse(
		items = items.map { it.toV2() },
		page = page,
		size = size,
		totalElements = totalElements,
		totalPages = totalPages,
	)

private fun PostAuthorResponse.toV2(): V2PostAuthorResponse =
	V2PostAuthorResponse(
		id = id,
		nickname = nickname,
		profileImageUrl = profileImageUrl,
	)

private fun PostShareResponse.toV2(): V2PostShareResponse =
	V2PostShareResponse(
		shareUrl = shareUrl,
		clientUrl = clientUrl,
		title = title,
		description = description,
		imageUrl = imageUrl,
	)
