package com.dwolla.cloudflare.domain.dto.ratelimits

case class RateLimitActionDTO (
  mode: String,
  timeout: Int,
  response: Option[RateLimitActionResponseDTO]
)

case class RateLimitActionResponseDTO (
  content_type: String,
  body: String
)
