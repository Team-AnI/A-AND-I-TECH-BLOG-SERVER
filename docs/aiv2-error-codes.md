# A&I v2 Error Code Reference

## Format
`[service 1자리][category 1자리][detail 3자리]`

## Service prefix
- `1`: Gateway
- `2`: Auth
- `3`: User
- `4`: Report
- `5`: Judge
- `6`: Blog
- `9`: Common

## Category prefix
- `0`: general
- `1`: authentication
- `2`: authorization
- `3`: validation
- `4`: business
- `5`: not found
- `6`: duplicate/conflict
- `7`: external system
- `8`: internal system

## Implemented in blog/common

| Code | Status | Service | Category | HTTP | Value | Severity |
|---:|---|---|---|---:|---|---|
| 90001 | deprecated | common | general | 400 | BAD_REQUEST | LOW |
| 90101 | active | common | authentication | 401 | AUTHENTICATE_REQUIRED | LOW |
| 90102 | active | common | authentication | 401 | AUTHENTICATE_INVALID | LOW |
| 90301 | active | common | general | 400 | DEVICE_OS_REQUIRED | LOW |
| 90302 | active | common | general | 400 | TIMESTAMP_REQUIRED | LOW |
| 90303 | active | common | general | 400 | TIMESTAMP_INVALID | LOW |
| 90304 | active | common | validation | 400 | MALFORMED_BODY | LOW |
| 90801 | deprecated | common | internal system | 500 | INTERNAL_SERVER_ERROR_DEPRECATED | CRITICAL |
| 93001 | active | common | validation | 400 | COMMON_VALIDATION_ERROR | LOW |
| 95001 | active | common | not found | 404 | COMMON_RESOURCE_NOT_FOUND | LOW |
| 90701 | active | common | external system | 502 | EXTERNAL_SYSTEM_ERROR | HIGH |
| 98801 | active | common | internal system | 500 | INTERNAL_SERVER_ERROR | CRITICAL |
| 60200 | active | blog | authorization | 403 | BLOG_ACCESS_DENIED | LOW |
| 60201 | active | blog | authorization | 403 | POST_EDIT_FORBIDDEN | LOW |
| 60202 | active | blog | authorization | 403 | COLLABORATOR_EDIT_FORBIDDEN | LOW |
| 60203 | active | blog | authorization | 403 | ADD_COLLABORATOR_FORBIDDEN | LOW |
| 60301 | active | blog | validation | 400 | BLOG_VALIDATION_ERROR | LOW |
| 60302 | active | blog | validation | 400 | PRIMARY_AUTHOR_IMMUTABLE | LOW |
| 60303 | active | blog | validation | 400 | TITLE_REQUIRED | LOW |
| 60304 | active | blog | validation | 400 | PUBLISHED_POST_CONTENT_REQUIRED | LOW |
| 60305 | active | blog | validation | 400 | OWNER_ALREADY_PRIMARY_AUTHOR | LOW |
| 60306 | active | blog | validation | 400 | COLLABORATOR_NICKNAME_REQUIRED | LOW |
| 60401 | active | blog | business | 400 | DRAFT_LIST_ONLY | LOW |
| 60501 | active | blog | not found | 404 | POST_NOT_FOUND | LOW |
| 60701 | active | blog | external system | 502 | IMAGE_UPLOAD_FAILED | HIGH |
| 64301 | active | blog | business | 400 | POST_NOT_EDITABLE | LOW |
| 64501 | active | blog | business | 403 | POST_NOT_PUBLISHED | LOW |
| 64801 | active | blog | internal system | 500 | POST_CREATE_FAILED | HIGH |
| 64802 | active | blog | internal system | 500 | POST_UPDATE_FAILED | HIGH |
| 64803 | active | blog | internal system | 500 | POST_DELETE_FAILED | HIGH |
| 64804 | active | blog | internal system | 500 | POST_PUBLISH_FAILED | HIGH |
| 64805 | active | blog | internal system | 500 | POST_UNPUBLISH_FAILED | HIGH |
| 68801 | active | blog | internal system | 500 | BLOG_INTERNAL_SERVER_ERROR | CRITICAL |

## Compatibility policy

- `90001 BAD_REQUEST` is kept only for backward compatibility with already published clients/log parsers.
- New generic validation or bad request fallback handling should prefer `93001 COMMON_VALIDATION_ERROR`.
- Operational monitoring and Discord Bot aggregation should focus on the v2.0.1 Common monitoring codes: `93001`, `95001`, `90701`, and `98801`.
- `90801 INTERNAL_SERVER_ERROR_DEPRECATED` is kept as deprecated. New Common internal server errors should use `98801 INTERNAL_SERVER_ERROR`.

## Confirm-needed items
- whether all GET endpoints must require `Authenticate`
- whether `success` is definitively `Boolean` in all A&I v2 services
- whether `salt` is informational or must participate in signature/replay protection
