package com.dwolla.cloudflare.domain.dto.ratelimits

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class RateLimitDTO (
  id: Option[String],
  disabled: Option[Boolean],
  description: Option[String],
  `match`: RateLimitMatchDTO,
  bypass: Option[Seq[RateLimitKeyValueDTO]],
  threshold: Int,
  period: Int,
  action: RateLimitActionDTO
) extends JsonWritable

case class RateLimitKeyValueDTO (
  name: String,
  value: String
) extends JsonWritable