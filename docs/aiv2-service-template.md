# A&I v2 Service Template for Auth/User/Report/Judge/Gateway

This repository contains the concrete implementation pattern for the Blog service. Apply the same additive migration pattern to other A&I services.

## Standard package layout

```text
common/api/v2/
  AiV2ApiResponse.kt
  AiV2RequestContextResolver.kt
  AiV2ExceptionHandler.kt
  AiV2ErrorCatalog.kt
  AiV2ErrorMapper.kt
presentation/
  <domain>/                # existing v1
presentation/v2/
  <domain>/                # new v2 controllers and DTOs
```

## Standard rules
1. never delete v1 endpoints
2. never mutate v1 request/response schema
3. add `/v2/**` endpoints only
4. keep service/usecase/domain shared if possible
5. isolate differences in controller/presenter layer

## Per-service URI recommendation
- Gateway: `/v2/gateway/**` or API edge `/api/v2/**`
- Auth: `/v2/auth/**`
- User: `/v2/users/**`
- Report: `/v2/reports/**`
- Judge: `/v2/judges/**` or `/v2/submissions/**`
- Blog: `/v2/posts/**`

## Per-service error prefix
- Gateway: `1xxxx`
- Auth: `2xxxx`
- User: `3xxxx`
- Report: `4xxxx`
- Judge: `5xxxx`
- Blog: `6xxxx`
- Common: `9xxxx`

## Suggested controller pattern
```kotlin
@RestController
@RequestMapping("/v2/<resource>")
class V2<Resource>Controller(
    private val service: <Resource>Service,
    private val requestContextResolver: AiV2RequestContextResolver,
) {
    @GetMapping("/{id}")
    suspend fun get(exchange: ServerWebExchange, @PathVariable id: String)
        : ResponseEntity<AiV2ApiResponse<V2<Resource>Response>> {
        requestContextResolver.resolve(exchange.request.headers)
        return ResponseEntity.ok(AiV2ApiResponse.success(service.get(id).toV2()))
    }
}
```

## Suggested exception strategy
- keep existing v1 `GlobalExceptionHandler`
- add new v2-only `@RestControllerAdvice(basePackageClasses = [...])`
- map `ResponseStatusException` / validation / parsing failures into v2 error object
- do not depend on HTTP status alone for client handling

## Suggested migration order for each service
1. identify current v1 controllers and DTOs
2. freeze v1 contracts
3. add common v2 envelope and error mapper
4. add v2 header resolver
5. add v2 controllers
6. add v2 response DTOs / mappers
7. write v1 + v2 coexistence tests
8. canary release one client

## Service-specific notes

### Auth
- likely endpoints: login, refresh, logout, revoke, token validation
- keep current token issue logic shared
- split only request/response and error codes

### User
- likely endpoints: me, profile update, lookup by id, avatar upload
- response wrapper and header policy should mirror blog

### Report
- likely endpoints: create report, list reports, status update, moderation action
- business validation errors should map to `44xxx`

### Judge
- likely endpoints: submit solution, get result, list submissions
- external judge/queue failures should map to `57xxx`

### Gateway
- if gateway transforms upstream errors, standardize all downstream failures into v2 envelope at edge
- preserve existing v1 pass-through routes until clients migrate
