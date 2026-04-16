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
	val missingDeviceOs = descriptor(HttpStatus.BAD_REQUEST, 90301, "deviceOS 헤더가 필요합니다.", "앱 정보를 확인할 수 없어요. 앱을 다시 실행해 주세요.")
	val missingAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90101, "Authenticate 헤더가 필요합니다.", "로그인이 필요해요. 다시 로그인해 주세요.")
	val invalidAuthenticate = descriptor(HttpStatus.UNAUTHORIZED, 90102, "유효한 Bearer 토큰이 필요합니다.", "로그인 정보가 유효하지 않아요. 다시 로그인해 주세요.")
	val missingTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90302, "timestamp 헤더가 필요합니다.", "요청 시간이 누락되었어요. 다시 시도해 주세요.")
	val invalidTimestamp = descriptor(HttpStatus.BAD_REQUEST, 90303, "timestamp 헤더는 ISO-8601 형식이어야 합니다.", "요청 시간이 올바르지 않아요. 다시 시도해 주세요.")
	val malformedBody = descriptor(HttpStatus.BAD_REQUEST, 90304, "요청 본문 형식이 올바르지 않습니다.", "보낸 데이터 형식이 올바르지 않아요. 다시 시도해 주세요.")
	val validationFailed = descriptor(HttpStatus.BAD_REQUEST, 60301, "입력값이 올바르지 않습니다.", "입력한 내용을 다시 확인해 주세요.")
	val forbidden = descriptor(HttpStatus.FORBIDDEN, 60200, "권한이 없습니다.", "이 작업을 수행할 권한이 없어요.")
	val postNotFound = descriptor(HttpStatus.NOT_FOUND, 60501, "게시글을 찾을 수 없습니다.", "요청한 글을 찾을 수 없어요.")
	val draftListOnly = descriptor(HttpStatus.BAD_REQUEST, 60401, "초안 글은 초안 목록에서만 조회할 수 있습니다.", "초안 글은 초안 목록에서만 볼 수 있어요.")
	val postEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60201, "게시글 작성자나 공동 작성자만 수정할 수 있습니다.", "이 글을 수정할 권한이 없어요.")
	val collaboratorEditForbidden = descriptor(HttpStatus.FORBIDDEN, 60202, "게시글 작성자만 공동 작성자를 수정할 수 있습니다.", "공동 작성자는 작성자만 변경할 수 있어요.")
	val addCollaboratorForbidden = descriptor(HttpStatus.FORBIDDEN, 60203, "게시글 작성자만 공동 작성자를 추가할 수 있습니다.", "공동 작성자는 작성자만 추가할 수 있어요.")
	val primaryAuthorImmutable = descriptor(HttpStatus.BAD_REQUEST, 60302, "주 작성자는 변경할 수 없습니다.", "주 작성자는 바꿀 수 없어요.")
	val titleRequired = descriptor(HttpStatus.BAD_REQUEST, 60303, "제목은 필수입니다.", "제목을 입력해 주세요.")
	val contentRequiredForPublished = descriptor(HttpStatus.BAD_REQUEST, 60304, "게시 상태에서는 본문 내용이 필요합니다.", "게시하려면 본문 내용을 입력해 주세요.")
	val ownerAlreadyPrimaryAuthor = descriptor(HttpStatus.BAD_REQUEST, 60305, "작성자는 이미 주 작성자입니다.", "이미 작성자로 등록된 사용자예요.")
	val collaboratorNicknameRequired = descriptor(HttpStatus.BAD_REQUEST, 60306, "신규 공동 작성자 닉네임이 필요합니다.", "공동 작성자 닉네임을 입력해 주세요.")
	val badRequest = descriptor(HttpStatus.BAD_REQUEST, 90001, "잘못된 요청입니다.", "요청을 처리할 수 없어요. 다시 시도해 주세요.")
	val s3UploadFailed = descriptor(HttpStatus.BAD_GATEWAY, 60701, "이미지 업로드에 실패했습니다.", "이미지 업로드에 실패했어요. 잠시 후 다시 시도해 주세요.")
	val externalSystemUnavailable = descriptor(HttpStatus.SERVICE_UNAVAILABLE, 90701, "외부 시스템을 사용할 수 없습니다.", "일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.")
	val internalServerError = descriptor(HttpStatus.INTERNAL_SERVER_ERROR, 90801, "서버 내부 오류가 발생했습니다.", "문제가 발생했어요. 잠시 후 다시 시도해 주세요.")

	private fun descriptor(status: HttpStatus, code: Int, message: String, alert: String) =
		AiV2ErrorDescriptor(
			httpStatus = status,
			code = code,
			message = message,
			alert = alert,
		)
}
