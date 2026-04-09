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

| Code | Meaning | Source |
|---|---|---|
| 90101 | Authenticate header is required | common |
| 90102 | Authenticate header invalid | common |
| 90301 | deviceOS header is required | common |
| 90302 | timestamp header is required | common |
| 90303 | timestamp format invalid | common |
| 90304 | request body invalid | common |
| 90801 | internal server error | common |
| 60200 | forbidden | blog |
| 60201 | only post owner or collaborator can edit | blog |
| 60202 | only post owner can modify collaborators | blog |
| 60203 | only post owner can add collaborators | blog |
| 60301 | validation failed | blog |
| 60302 | primary author cannot be changed | blog |
| 60303 | title is required | blog |
| 60304 | contentMarkdown is required for published post | blog |
| 60305 | owner is already the primary author | blog |
| 60306 | collaborator nickname is required for new user | blog |
| 60401 | draft posts are only available in draft list | blog |
| 60501 | post not found | blog |
| 60701 | image upload failed | blog |
| 90701 | external system unavailable | common |

## Confirm-needed items
- whether all GET endpoints must require `Authenticate`
- whether `success` is definitively `Boolean` in all A&I v2 services
- whether `salt` is informational or must participate in signature/replay protection
