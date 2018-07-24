package com.dwolla.cloudflare.domain.dto.ratelimits

case class RateLimitDTO (
  id: Option[String],
  disabled: Option[Boolean],
  description: Option[String],
  `match`: RateLimitMatchDTO,
  bypass: Option[Seq[RateLimitKeyValueDTO]],
  threshold: Int,
  period: Int,
  action: RateLimitActionDTO
)

case class RateLimitKeyValueDTO (
  name: String,
  value: String
)
