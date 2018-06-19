package com.dwolla.cloudflare.domain.dto.ratelimits

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class RateLimitMatchDTO (
  request: RateLimitMatchRequestDTO,
  response: Option[RateLimitMatchResponseDTO]
) extends JsonWritable

case class RateLimitMatchRequestDTO (
  methods: Option[Seq[String]],
  schemes: Option[Seq[String]],
  url: String
) extends JsonWritable

case class RateLimitMatchResponseDTO (
  status: Option[Seq[Int]],
  origin_traffic: Option[Boolean],
  headers: Seq[RateLimitMatchResponseHeaderDTO]
) extends JsonWritable

case class RateLimitMatchResponseHeaderDTO (
  name: String,
  op: String,
  value: String
) extends JsonWritable