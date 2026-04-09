package com.aandiclub.tech.blog.common.api.v2

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

data class AiV2ErrorDescriptor(
	val httpStatus: HttpStatusCode,
	val code: Int,
	val message: String,
	val alert: String,
)

class AiV2ProtocolException(
	val descriptor: AiV2ErrorDescriptor,
	val value: String = descriptor.message,
	cause: Throwable? = null,
) : RuntimeException(descriptor.message, cause)

object AiV2ErrorCatalog {
	val missingDeviceOs = descriptor(HttpStatus.BAD_REQUEST, 90301, "deviceOS header is required", "deviceOS header is required")
	val missingAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90101, "Authenticate header is required", "Authenticate header is required")
	val invalidAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90102, "Authenticate header must contain a valid Bearer token", "Authenticate header must contain a valid Bearer token")
	val missingTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90302, "timestamp header is required", "timestamp header is required")
	val invalidTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90303, "timestamp header must be ISO-8601 instant", "timestamp header must be ISO-8601 instant")
	val malformedBody = descriptor(HttpStatus.BAD_REQUEST, 90304, "request body is invalid", "request body is invalid")
	val validationFailed = descriptor(HttpStatus.BAD_REQUEST, 60301, "validation failed", "validation failed")
	val forbidden = descriptor(HttpStatus.FORBIDDEN, 60200, "forbidden", "forbidden")
	val postNotFound = descriptor(HttpStatus.NOT_FOUND, 60501, "post not found", "post not found")
	val draftListOnly = descriptor(HttpStatus.BAD_REQUEST, 60401, "draft posts are only available in draft list", "draft posts are only available in draft list")
	val postEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60201, "only post owner or collaborator can edit", "only post owner or collaborator can edit")
	val collaboratorEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60202, "only post owner can modify collaborators", "only post owner can modify collaborators")
	val addCollaboratorForbidden = descriptor(HttpStatus.FORBIDDEN, 60203, "only post owner can add collaborators", "only post owner can add collaborators")
	val primaryAuthorImmutable = descriptor(HttpStatus.BAD_REQUEST, 60302, "primary author cannot be changed", "primary author cannot be changed")
	val titleRequired = descriptor(HttpStatus.BAD_REQUEST, 60303, "title is required", "title is required")
	val contentRequiredForPublished = descriptor(HttpStatus.BAD_REQUEST, 60304, "contentMarkdown is required for published post", "contentMarkdown is required for published post")
	val ownerAlreadyPrimaryAuthor = descriptor(HttpStatus.BAD_REQUEST, 60305, "owner is already the primary author", "owner is already the primary author")
	val collaboratorNicknameRequired = descriptor(HttpStatus.BAD_REQUEST, 60306, "collaborator nickname is required for new user", "collaborator nickname is required for new user")
	val badRequest = descriptor(HttpStatus.BAD_REQUEST, 90001, "bad request", "bad request")
	val s3UploadFailed = descriptor(HttpStatus.BAD_GATEWAY, 60701, "image upload failed", "image upload failed")
	val externalSystemUnavailable = descriptor(HttpStatus.SERVICE_UNAVAILABLE, 90701, "external system unavailable", "external system unavailable")
	val internalServerError = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 90801, "internal server error", "internal server error")

	private fun descriptor(status: HttpStatus, code: Int, message: String, alert: String) =
		AiV2ErrorDescriptor(
			httpStatus = status,
			code = code,
			message = message,
			alert = alert,
		)
}
