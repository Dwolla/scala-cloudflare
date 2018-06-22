package com.dwolla.cloudflare.domain.model.ratelimits

case class RateLimitAction (
  mode: String,
  timeout: Int,
  response: Option[RateLimitActionResponse] = None
)

case class RateLimitActionResponse (
  contentType: String,
  body: String
)
