package com.aandiclub.tech.blog.common.api.v2

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

enum class AiV2ErrorStatus {
	ACTIVE,
	PLANNED,
	DEPRECATED,
}

enum class AiV2ErrorService(
	val domain: String,
	val domainCode: Int,
) {
	BLOG("blog", 6),
	COMMON("common", 9),
}

enum class AiV2ErrorCategory {
	GENERAL,
	AUTHENTICATION,
	AUTHORIZATION,
	VALIDATION,
	BUSINESS,
	RESOURCE_NOT_FOUND,
	CONFLICT,
	EXTERNAL_SYSTEM,
	INTERNAL_SYSTEM,
}

enum class AiV2ErrorSeverity {
	LOW,
	MEDIUM,
	HIGH,
	CRITICAL,
}

data class AiV2ErrorDescriptor(
	val httpStatus: HttpStatusCode,
	val code: Int,
	val status: AiV2ErrorStatus,
	val service: AiV2ErrorService,
	val category: AiV2ErrorCategory,
	val value: String,
	val message: String,
	val alert: String,
	val severity: AiV2ErrorSeverity,
)

class AiV2ProtocolException(
	val descriptor: AiV2ErrorDescriptor,
	val messageOverride: String? = null,
	cause: Throwable? = null,
) : RuntimeException(messageOverride ?: descriptor.message, cause)

object AiV2ErrorCatalog {
	val missingDeviceOs = descriptor(HttpStatus.BAD_REQUEST, 90301, AiV2ErrorService.COMMON, AiV2ErrorCategory.GENERAL, "DEVICE_OS_REQUIRED", "deviceOS 헤더가 필요합니다.", "앱 정보를 확인할 수 없어요. 앱을 다시 실행해 주세요.", AiV2ErrorSeverity.LOW)
	val missingAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90101, AiV2ErrorService.COMMON, AiV2ErrorCategory.AUTHENTICATION, "AUTHENTICATE_REQUIRED", "Authenticate 헤더가 필요합니다.", "로그인이 필요해요. 다시 로그인해 주세요.", AiV2ErrorSeverity.LOW)
	val invalidAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90102, AiV2ErrorService.COMMON, AiV2ErrorCategory.AUTHENTICATION, "AUTHENTICATE_INVALID", "유효한 Bearer 토큰이 필요합니다.", "로그인 정보가 유효하지 않아요. 다시 로그인해 주세요.", AiV2ErrorSeverity.LOW)
	val missingTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90302, AiV2ErrorService.COMMON, AiV2ErrorCategory.GENERAL, "TIMESTAMP_REQUIRED", "timestamp 헤더가 필요합니다.", "요청 시간이 누락되었어요. 다시 시도해 주세요.", AiV2ErrorSeverity.LOW)
	val invalidTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90303, AiV2ErrorService.COMMON, AiV2ErrorCategory.GENERAL, "TIMESTAMP_INVALID", "timestamp 헤더는 ISO-8601 형식이어야 합니다.", "요청 시간이 올바르지 않아요. 다시 시도해 주세요.", AiV2ErrorSeverity.LOW)
	val malformedBody = descriptor(HttpStatus.BAD_REQUEST, 90304, AiV2ErrorService.COMMON, AiV2ErrorCategory.VALIDATION, "MALFORMED_BODY", "요청 본문 형식이 올바르지 않습니다.", "보낸 데이터 형식이 올바르지 않아요. 다시 시도해 주세요.", AiV2ErrorSeverity.LOW)
	val validationFailed = descriptor(HttpStatus.BAD_REQUEST, 60301, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "BLOG_VALIDATION_ERROR", "입력값이 올바르지 않습니다.", "입력한 내용을 다시 확인해 주세요.", AiV2ErrorSeverity.LOW)
	val forbidden = descriptor(HttpStatus.FORBIDDEN, 60200, AiV2ErrorService.BLOG, AiV2ErrorCategory.AUTHORIZATION, "BLOG_ACCESS_DENIED", "권한이 없습니다.", "이 작업을 수행할 권한이 없어요.", AiV2ErrorSeverity.LOW)
	val postNotFound = descriptor(HttpStatus.NOT_FOUND, 60501, AiV2ErrorService.BLOG, AiV2ErrorCategory.RESOURCE_NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.", "요청한 글을 찾을 수 없어요.", AiV2ErrorSeverity.LOW)
	val draftListOnly = descriptor(HttpStatus.BAD_REQUEST, 60401, AiV2ErrorService.BLOG, AiV2ErrorCategory.BUSINESS, "DRAFT_LIST_ONLY", "초안 글은 초안 목록에서만 조회할 수 있습니다.", "초안 글은 초안 목록에서만 볼 수 있어요.", AiV2ErrorSeverity.LOW)
	val postEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60201, AiV2ErrorService.BLOG, AiV2ErrorCategory.AUTHORIZATION, "POST_EDIT_FORBIDDEN", "게시글 작성자나 공동 작성자만 수정할 수 있습니다.", "이 글을 수정할 권한이 없어요.", AiV2ErrorSeverity.LOW)
	val collaboratorEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60202, AiV2ErrorService.BLOG, AiV2ErrorCategory.AUTHORIZATION, "COLLABORATOR_EDIT_FORBIDDEN", "게시글 작성자만 공동 작성자를 수정할 수 있습니다.", "공동 작성자는 작성자만 변경할 수 있어요.", AiV2ErrorSeverity.LOW)
	val addCollaboratorForbidden = descriptor(HttpStatus.FORBIDDEN, 60203, AiV2ErrorService.BLOG, AiV2ErrorCategory.AUTHORIZATION, "ADD_COLLABORATOR_FORBIDDEN", "게시글 작성자만 공동 작성자를 추가할 수 있습니다.", "공동 작성자는 작성자만 추가할 수 있어요.", AiV2ErrorSeverity.LOW)
	val primaryAuthorImmutable = descriptor(HttpStatus.BAD_REQUEST, 60302, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "PRIMARY_AUTHOR_IMMUTABLE", "주 작성자는 변경할 수 없습니다.", "주 작성자는 바꿀 수 없어요.", AiV2ErrorSeverity.LOW)
	val titleRequired = descriptor(HttpStatus.BAD_REQUEST, 60303, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "TITLE_REQUIRED", "제목은 필수입니다.", "제목을 입력해 주세요.", AiV2ErrorSeverity.LOW)
	val contentRequiredForPublished = descriptor(HttpStatus.BAD_REQUEST, 60304, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "PUBLISHED_POST_CONTENT_REQUIRED", "게시 상태에서는 본문 내용이 필요합니다.", "게시하려면 본문 내용을 입력해 주세요.", AiV2ErrorSeverity.LOW)
	val ownerAlreadyPrimaryAuthor = descriptor(HttpStatus.BAD_REQUEST, 60305, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "OWNER_ALREADY_PRIMARY_AUTHOR", "작성자는 이미 주 작성자입니다.", "이미 작성자로 등록된 사용자예요.", AiV2ErrorSeverity.LOW)
	val collaboratorNicknameRequired = descriptor(HttpStatus.BAD_REQUEST, 60306, AiV2ErrorService.BLOG, AiV2ErrorCategory.VALIDATION, "COLLABORATOR_NICKNAME_REQUIRED", "신규 공동 작성자 닉네임이 필요합니다.", "공동 작성자 닉네임을 입력해 주세요.", AiV2ErrorSeverity.LOW)
	val badRequest = descriptor(HttpStatus.BAD_REQUEST, 90001, AiV2ErrorService.COMMON, AiV2ErrorCategory.GENERAL, "BAD_REQUEST", "잘못된 요청입니다.", "요청을 처리할 수 없어요. 다시 시도해 주세요.", AiV2ErrorSeverity.LOW)
	val s3UploadFailed = descriptor(HttpStatus.BAD_GATEWAY, 60701, AiV2ErrorService.BLOG, AiV2ErrorCategory.EXTERNAL_SYSTEM, "IMAGE_UPLOAD_FAILED", "이미지 업로드에 실패했습니다.", "이미지 업로드에 실패했어요. 잠시 후 다시 시도해 주세요.", AiV2ErrorSeverity.HIGH)
	val externalSystemUnavailable = descriptor(HttpStatus.BAD_GATEWAY, 90701, AiV2ErrorService.COMMON, AiV2ErrorCategory.EXTERNAL_SYSTEM, "EXTERNAL_SYSTEM_ERROR", "외부 시스템 오류가 발생했습니다.", "외부 시스템 처리 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val deprecatedInternalServerError = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 90801, AiV2ErrorService.COMMON, AiV2ErrorCategory.INTERNAL_SYSTEM, "INTERNAL_SERVER_ERROR_DEPRECATED", "서버 내부 오류가 발생했습니다.", "문제가 발생했어요. 잠시 후 다시 시도해 주세요.", AiV2ErrorSeverity.CRITICAL, AiV2ErrorStatus.DEPRECATED)

	val postNotEditable = descriptor(HttpStatus.BAD_REQUEST, 64301, AiV2ErrorService.BLOG, AiV2ErrorCategory.BUSINESS, "POST_NOT_EDITABLE", "수정할 수 없는 게시글입니다.", "수정할 수 없는 게시글입니다.", AiV2ErrorSeverity.LOW)
	val postNotPublished = descriptor(HttpStatus.FORBIDDEN, 64501, AiV2ErrorService.BLOG, AiV2ErrorCategory.BUSINESS, "POST_NOT_PUBLISHED", "게시글이 아직 공개되지 않았습니다.", "아직 공개되지 않은 게시글입니다.", AiV2ErrorSeverity.LOW)
	val postCreateFailed = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 64801, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "POST_CREATE_FAILED", "게시글 생성에 실패했습니다.", "게시글 생성 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val postUpdateFailed = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 64802, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "POST_UPDATE_FAILED", "게시글 수정에 실패했습니다.", "게시글 수정 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val postDeleteFailed = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 64803, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "POST_DELETE_FAILED", "게시글 삭제에 실패했습니다.", "게시글 삭제 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val postPublishFailed = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 64804, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "POST_PUBLISH_FAILED", "게시글 공개에 실패했습니다.", "게시글 공개 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val postUnpublishFailed = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 64805, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "POST_UNPUBLISH_FAILED", "게시글 비공개 처리에 실패했습니다.", "게시글 비공개 처리 중 오류가 발생했습니다.", AiV2ErrorSeverity.HIGH)
	val blogInternalServerError = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 68801, AiV2ErrorService.BLOG, AiV2ErrorCategory.INTERNAL_SYSTEM, "BLOG_INTERNAL_SERVER_ERROR", "예상하지 못한 블로그 서버 오류가 발생했습니다.", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", AiV2ErrorSeverity.CRITICAL)

	val commonValidationError = descriptor(HttpStatus.BAD_REQUEST, 93001, AiV2ErrorService.COMMON, AiV2ErrorCategory.VALIDATION, "COMMON_VALIDATION_ERROR", "요청 값이 올바르지 않습니다.", "입력값 형식이 올바르지 않습니다.", AiV2ErrorSeverity.LOW)
	val commonResourceNotFound = descriptor(HttpStatus.NOT_FOUND, 95001, AiV2ErrorService.COMMON, AiV2ErrorCategory.RESOURCE_NOT_FOUND, "COMMON_RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", "요청한 리소스를 찾을 수 없습니다.", AiV2ErrorSeverity.LOW)
	val internalServerError = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 98801, AiV2ErrorService.COMMON, AiV2ErrorCategory.INTERNAL_SYSTEM, "INTERNAL_SERVER_ERROR", "예상하지 못한 서버 오류가 발생했습니다.", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", AiV2ErrorSeverity.CRITICAL)

	val all: List<AiV2ErrorDescriptor> = listOf(
		missingDeviceOs,
		missingAuthenticate,
		invalidAuthenticate,
		missingTimestamp,
		invalidTimestamp,
		malformedBody,
		validationFailed,
		forbidden,
		postNotFound,
		draftListOnly,
		postEditForbidden,
		collaboratorEditForbidden,
		addCollaboratorForbidden,
		primaryAuthorImmutable,
		titleRequired,
		contentRequiredForPublished,
		ownerAlreadyPrimaryAuthor,
		collaboratorNicknameRequired,
		badRequest,
		s3UploadFailed,
		externalSystemUnavailable,
		deprecatedInternalServerError,
		postNotEditable,
		postNotPublished,
		postCreateFailed,
		postUpdateFailed,
		postDeleteFailed,
		postPublishFailed,
		postUnpublishFailed,
		blogInternalServerError,
		commonValidationError,
		commonResourceNotFound,
		internalServerError,
	)

	private fun descriptor(
		httpStatus: HttpStatus,
		code: Int,
		service: AiV2ErrorService,
		category: AiV2ErrorCategory,
		value: String,
		message: String,
		alert: String,
		severity: AiV2ErrorSeverity,
		status: AiV2ErrorStatus = AiV2ErrorStatus.ACTIVE,
	) =
		AiV2ErrorDescriptor(
			httpStatus = httpStatus,
			code = code,
			status = status,
			service = service,
			category = category,
			value = value,
			message = message,
			alert = alert,
			severity = severity,
		)
}
