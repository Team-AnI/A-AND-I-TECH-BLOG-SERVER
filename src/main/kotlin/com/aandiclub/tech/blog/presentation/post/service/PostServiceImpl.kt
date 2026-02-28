package com.aandiclub.tech.blog.presentation.post.service

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostCollaborator
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.user.User
import com.aandiclub.tech.blog.infrastructure.post.PostCollaboratorRepository
import com.aandiclub.tech.blog.infrastructure.post.PostRepository
import com.aandiclub.tech.blog.infrastructure.user.UserRepository
import com.aandiclub.tech.blog.presentation.post.dto.AddCollaboratorRequest
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil

@Service
class PostServiceImpl(
	private val postRepository: PostRepository,
	private val postCollaboratorRepository: PostCollaboratorRepository,
	private val userRepository: UserRepository,
	private val entityOperations: R2dbcEntityOperations,
) : PostService {

	override suspend fun create(request: CreatePostRequest): PostResponse {
		val upsertedAuthor = upsertAuthorFromRequest(request.author)
		val post = Post(
			title = request.title,
			contentMarkdown = request.contentMarkdown,
			thumbnailUrl = request.thumbnailUrl,
			authorId = request.author.id,
			status = request.status,
		)
		val saved = entityOperations.insert(post).awaitSingle()
		return saved.toResponse(
			author = upsertedAuthor?.toAuthorResponse() ?: resolveAuthor(request.author.id, request.author.nickname, request.author.profileImageUrl),
		)
	}

	override suspend fun get(postId: UUID): PostResponse {
		val post = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		return buildPostResponse(post)
	}

	override suspend fun list(page: Int, size: Int, status: PostStatus?): PagedPostResponse {
		if (status == PostStatus.Draft) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "draft posts are only available in draft list")
		}
		return listByStatus(page, size, status ?: PostStatus.Published)
	}

	override suspend fun listDrafts(page: Int, size: Int): PagedPostResponse =
		listByStatus(page, size, PostStatus.Draft)

	private suspend fun listByStatus(page: Int, size: Int, status: PostStatus): PagedPostResponse {
		val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		val posts = postRepository.findByStatus(status, pageable).toList()
		val usersById = loadUsersById(posts.map { it.authorId }.toSet())
		val collaboratorsByPostId = loadCollaboratorsByPostId(posts.map { it.id }.toSet())
		val items = posts.map { post ->
			post.toResponse(
				author = usersById[post.authorId]?.toAuthorResponse() ?: fallbackAuthor(post.authorId),
				collaborators = collaboratorsByPostId[post.id] ?: emptyList(),
			)
		}
		val totalElements = postRepository.countByStatus(status)
		val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size.toDouble()).toInt()

		return PagedPostResponse(
			items = items,
			page = page,
			size = size,
			totalElements = totalElements,
			totalPages = totalPages,
		)
	}

	override suspend fun patch(postId: UUID, requesterId: String, request: PatchPostRequest): PostResponse {
		val current = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		if (!canEdit(current.id, current.authorId, requesterId)) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner or collaborator can edit")
		}
		if (request.author != null && request.author.id != current.authorId) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "primary author cannot be changed")
		}
		val upsertedAuthor = request.author?.let { upsertAuthorFromRequest(it) }
		val updated = current.copy(
			title = request.title ?: current.title,
			contentMarkdown = request.contentMarkdown ?: current.contentMarkdown,
			thumbnailUrl = request.thumbnailUrl ?: current.thumbnailUrl,
			authorId = current.authorId,
			status = request.status ?: current.status,
			updatedAt = Instant.now(),
		)
		val saved = postRepository.save(updated)
		val collaborators = loadCollaborators(saved.id)
		return saved.toResponse(
			author = upsertedAuthor?.toAuthorResponse()
				?: resolveAuthor(saved.authorId, request.author?.nickname, request.author?.profileImageUrl),
			collaborators = collaborators,
		)
	}

	override suspend fun addCollaborator(postId: UUID, requesterId: String, request: AddCollaboratorRequest): PostResponse {
		val post = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		val actorId = normalizeRequesterId(requesterId)
		request.ownerId?.trim()?.takeIf { it.isNotEmpty() }?.let { claimedOwnerId ->
			if (claimedOwnerId != actorId) {
				throw ResponseStatusException(HttpStatus.FORBIDDEN, "requester id does not match ownerId")
			}
		}
		if (post.authorId != actorId) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner can add collaborators")
		}
		val collaboratorId = request.collaborator.id
		if (collaboratorId == post.authorId) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "owner is already the primary author")
		}
		if (upsertAuthorFromRequest(request.collaborator) == null) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "collaborator nickname is required for new user")
		}
		if (!postCollaboratorRepository.existsByPostIdAndUserId(postId, collaboratorId)) {
			postCollaboratorRepository.save(
				PostCollaborator(
					postId = postId,
					userId = collaboratorId,
				),
			)
		}
		return buildPostResponse(post)
	}

	private suspend fun canEdit(postId: UUID, ownerId: String, requesterId: String): Boolean {
		val actorId = normalizeRequesterId(requesterId)
		if (actorId == ownerId) return true
		return postCollaboratorRepository.existsByPostIdAndUserId(postId, actorId)
	}

	private fun normalizeRequesterId(requesterId: String): String {
		val actorId = requesterId.trim()
		if (actorId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "requester id is required")
		}
		return actorId
	}

	override suspend fun delete(postId: UUID) {
		val current = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		postRepository.save(
			current.copy(
				status = PostStatus.Deleted,
				updatedAt = Instant.now(),
			),
		)
	}

	private fun notFound(postId: UUID): ResponseStatusException =
		ResponseStatusException(HttpStatus.NOT_FOUND, "post not found: $postId")

	private suspend fun loadUsersById(authorIds: Set<String>): Map<String, User> {
		if (authorIds.isEmpty()) return emptyMap()
		return userRepository.findAllById(authorIds).toList().associateBy { it.id }
	}

	private suspend fun upsertAuthorFromRequest(author: PostAuthorRequest): User? {
		val existing = userRepository.findById(author.id)
		val nickname = author.nickname?.takeIf { it.isNotBlank() } ?: existing?.nickname
		val thumbnailUrl = author.profileImageUrl ?: existing?.thumbnailUrl

		// Legacy payload(authorId string) can miss nickname; skip upsert in that case.
		if (nickname == null) return existing

		if (existing == null) {
			return userRepository.save(
				User(
					id = author.id,
					nickname = nickname,
					thumbnailUrl = thumbnailUrl,
				),
			)
		}

		if (existing.nickname == nickname && existing.thumbnailUrl == thumbnailUrl) {
			return existing
		}

		return userRepository.save(
			existing.copy(
				nickname = nickname,
				thumbnailUrl = thumbnailUrl,
				updatedAt = Instant.now(),
			),
		)
	}

	private suspend fun loadCollaborators(postId: UUID): List<PostAuthorResponse> {
		val collaborators = postCollaboratorRepository.findByPostId(postId).toList()
		if (collaborators.isEmpty()) return emptyList()
		val usersById = loadUsersById(collaborators.map { it.userId }.toSet())
		return collaborators.map { collaborator ->
			usersById[collaborator.userId]?.toAuthorResponse() ?: fallbackAuthor(collaborator.userId)
		}
	}

	private suspend fun loadCollaboratorsByPostId(postIds: Set<UUID>): Map<UUID, List<PostAuthorResponse>> {
		if (postIds.isEmpty()) return emptyMap()
		val collaborators = postCollaboratorRepository.findByPostIdIn(postIds).toList()
		if (collaborators.isEmpty()) return emptyMap()
		val usersById = loadUsersById(collaborators.map { it.userId }.toSet())
		return collaborators.groupBy { it.postId }.mapValues { (_, values) ->
			values.map { collaborator ->
				usersById[collaborator.userId]?.toAuthorResponse() ?: fallbackAuthor(collaborator.userId)
			}
		}
	}

	private suspend fun buildPostResponse(post: Post): PostResponse {
		val author = resolveAuthor(post.authorId)
		val collaborators = loadCollaborators(post.id)
		return post.toResponse(
			author = author,
			collaborators = collaborators,
		)
	}

	private suspend fun resolveAuthor(
		authorId: String,
		fallbackNickname: String? = null,
		fallbackThumbnailUrl: String? = null,
	): PostAuthorResponse =
		userRepository.findById(authorId)?.toAuthorResponse()
			?: fallbackAuthor(authorId, fallbackNickname, fallbackThumbnailUrl)

	private fun User.toAuthorResponse() = PostAuthorResponse(
		id = id,
		nickname = nickname,
		profileImageUrl = thumbnailUrl,
	)

	private fun fallbackAuthor(
		authorId: String,
		nickname: String? = null,
		thumbnailUrl: String? = null,
	) = PostAuthorResponse(
		id = authorId,
		nickname = nickname?.takeIf { it.isNotBlank() } ?: "unknown",
		profileImageUrl = thumbnailUrl,
	)

	private fun Post.toResponse(
		author: PostAuthorResponse,
		collaborators: List<PostAuthorResponse> = emptyList(),
	) = PostResponse(
		id = id,
		title = title,
		contentMarkdown = contentMarkdown,
		thumbnailUrl = thumbnailUrl,
		author = author,
		collaborators = collaborators,
		status = status,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)
}
