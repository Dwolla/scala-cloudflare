package com.dwolla.cloudflare.domain.dto.ratelimits

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class RateLimitActionDTO (
  mode: String,
  timeout: Int,
  response: Option[RateLimitActionResponseDTO]
) extends JsonWritable

case class RateLimitActionResponseDTO (
  content_type: String,
  body: String
) extends JsonWritable
