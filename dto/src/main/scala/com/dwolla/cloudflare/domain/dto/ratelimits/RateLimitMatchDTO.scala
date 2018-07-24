package com.dwolla.cloudflare.domain.dto.ratelimits

case class RateLimitMatchDTO (
  request: RateLimitMatchRequestDTO,
  response: Option[RateLimitMatchResponseDTO]
)

case class RateLimitMatchRequestDTO (
  methods: Option[Seq[String]],
  schemes: Option[Seq[String]],
  url: String
)

case class RateLimitMatchResponseDTO (
  status: Option[Seq[Int]],
  origin_traffic: Option[Boolean],
  headers: Seq[RateLimitMatchResponseHeaderDTO]
)

case class RateLimitMatchResponseHeaderDTO (
  name: String,
  op: String,
  value: String
)
