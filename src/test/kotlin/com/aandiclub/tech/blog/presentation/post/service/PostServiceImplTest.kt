package com.aandiclub.tech.blog.presentation.post.service

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostCollaborator
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.domain.user.User
import com.aandiclub.tech.blog.infrastructure.post.PostCollaboratorRepository
import com.aandiclub.tech.blog.infrastructure.post.PostRepository
import com.aandiclub.tech.blog.infrastructure.user.UserRepository
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

class PostServiceImplTest : StringSpec({
	"patch should allow collaborator save when collaborators payload is unchanged" {
		val postRepository = mockk<PostRepository>()
		val postCollaboratorRepository = mockk<PostCollaboratorRepository>()
		val userRepository = mockk<UserRepository>()
		val entityOperations = mockk<R2dbcEntityOperations>(relaxed = true)
		val service = PostServiceImpl(postRepository, postCollaboratorRepository, userRepository, entityOperations)

		val postId = UUID.randomUUID()
		val ownerId = "u-owner-1"
		val collaboratorId = "u-collab-1"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val current = Post(
			id = postId,
			title = "draft title",
			contentMarkdown = "draft content",
			authorId = ownerId,
			status = PostStatus.Draft,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) } returns current
		coEvery { postCollaboratorRepository.existsByPostIdAndUserId(postId, collaboratorId) } returns true
		every { postCollaboratorRepository.findByPostId(postId) } returns
			flowOf(
				PostCollaborator(
					postId = postId,
					userId = collaboratorId,
				),
			)
		coEvery { postRepository.save(any()) } answers { firstArg() }
		coEvery { userRepository.findById(ownerId) } returns User(id = ownerId, nickname = "owner")
		every { userRepository.findAllById(setOf(collaboratorId)) } returns
			flowOf(User(id = collaboratorId, nickname = "collab"))

		val response = service.patch(
			postId = postId,
			requesterId = collaboratorId,
			request = PatchPostRequest(
				title = "updated title",
				status = PostStatus.Draft,
				collaborators = listOf(PostAuthorRequest(id = collaboratorId)),
			),
		)

		response.title shouldBe "updated title"
		response.status shouldBe PostStatus.Draft
		response.collaborators.map { it.id } shouldBe listOf(collaboratorId)
		coVerify(exactly = 1) { postRepository.save(any()) }
	}

	"patch should reject collaborator when collaborators payload actually changes" {
		val postRepository = mockk<PostRepository>()
		val postCollaboratorRepository = mockk<PostCollaboratorRepository>()
		val userRepository = mockk<UserRepository>()
		val entityOperations = mockk<R2dbcEntityOperations>(relaxed = true)
		val service = PostServiceImpl(postRepository, postCollaboratorRepository, userRepository, entityOperations)

		val postId = UUID.randomUUID()
		val ownerId = "u-owner-2"
		val collaboratorId = "u-collab-2"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val current = Post(
			id = postId,
			title = "draft title",
			contentMarkdown = "draft content",
			authorId = ownerId,
			status = PostStatus.Draft,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) } returns current
		coEvery { postCollaboratorRepository.existsByPostIdAndUserId(postId, collaboratorId) } returns true
		every { postCollaboratorRepository.findByPostId(postId) } returns
			flowOf(
				PostCollaborator(
					postId = postId,
					userId = collaboratorId,
				),
			)

		val exception = shouldThrow<ResponseStatusException> {
			service.patch(
				postId = postId,
				requesterId = collaboratorId,
				request = PatchPostRequest(
					title = "updated title",
					status = PostStatus.Draft,
					collaborators = listOf(PostAuthorRequest(id = "u-other-collab")),
				),
			)
		}

		exception.statusCode shouldBe HttpStatus.FORBIDDEN
		coVerify(exactly = 0) { postRepository.save(any()) }
	}

	"patch should store request summary when provided" {
		val postRepository = mockk<PostRepository>()
		val postCollaboratorRepository = mockk<PostCollaboratorRepository>()
		val userRepository = mockk<UserRepository>()
		val entityOperations = mockk<R2dbcEntityOperations>(relaxed = true)
		val service = PostServiceImpl(postRepository, postCollaboratorRepository, userRepository, entityOperations)

		val postId = UUID.randomUUID()
		val ownerId = "u-owner-3"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val current = Post(
			id = postId,
			title = "original title",
			summary = "original summary",
			contentMarkdown = "original content",
			authorId = ownerId,
			status = PostStatus.Published,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) } returns current
		coEvery { postRepository.save(any()) } answers { firstArg() }
		coEvery { userRepository.findById(ownerId) } returns User(id = ownerId, nickname = "owner")
		every { postCollaboratorRepository.findByPostId(postId) } returns flowOf()

		val response = service.patch(
			postId = postId,
			requesterId = ownerId,
			request = PatchPostRequest(
				summary = "custom summary",
				contentMarkdown = "## Kotlin Summary\n\n본문  내용",
				status = PostStatus.Published,
			),
		)

		response.summary shouldBe "custom summary"
	}

	"patch should set summary to empty when summary is missing" {
		val postRepository = mockk<PostRepository>()
		val postCollaboratorRepository = mockk<PostCollaboratorRepository>()
		val userRepository = mockk<UserRepository>()
		val entityOperations = mockk<R2dbcEntityOperations>(relaxed = true)
		val service = PostServiceImpl(postRepository, postCollaboratorRepository, userRepository, entityOperations)

		val postId = UUID.randomUUID()
		val ownerId = "u-owner-4"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val current = Post(
			id = postId,
			title = "old draft title",
			summary = "old draft summary",
			contentMarkdown = " ",
			authorId = ownerId,
			status = PostStatus.Draft,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) } returns current
		coEvery { postRepository.save(any()) } answers { firstArg() }
		coEvery { userRepository.findById(ownerId) } returns User(id = ownerId, nickname = "owner")
		every { postCollaboratorRepository.findByPostId(postId) } returns flowOf()

		val response = service.patch(
			postId = postId,
			requesterId = ownerId,
			request = PatchPostRequest(
				title = "new draft title",
				contentMarkdown = " ",
				status = PostStatus.Draft,
			),
		)

		response.summary shouldBe ""
	}

	"patch should update post type when requested" {
		val postRepository = mockk<PostRepository>()
		val postCollaboratorRepository = mockk<PostCollaboratorRepository>()
		val userRepository = mockk<UserRepository>()
		val entityOperations = mockk<R2dbcEntityOperations>(relaxed = true)
		val service = PostServiceImpl(postRepository, postCollaboratorRepository, userRepository, entityOperations)

		val postId = UUID.randomUUID()
		val ownerId = "u-owner-5"
		val now = Instant.parse("2026-02-15T12:00:00Z")
		val current = Post(
			id = postId,
			title = "blog title",
			contentMarkdown = "blog content",
			authorId = ownerId,
			type = PostType.Blog,
			status = PostStatus.Published,
			createdAt = now,
			updatedAt = now,
		)

		coEvery { postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) } returns current
		coEvery { postRepository.save(any()) } answers { firstArg() }
		coEvery { userRepository.findById(ownerId) } returns User(id = ownerId, nickname = "owner")
		every { postCollaboratorRepository.findByPostId(postId) } returns flowOf()

		val response = service.patch(
			postId = postId,
			requesterId = ownerId,
			request = PatchPostRequest(type = PostType.Lecture),
		)

		response.type shouldBe PostType.Lecture
	}
})
