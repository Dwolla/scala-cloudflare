package com.dwolla.cloudflare.domain.model.ratelimits

case class RateLimitMatch (
  request: RateLimitMatchRequest,
  response: Option[RateLimitMatchResponse] = None
)

case class RateLimitMatchRequest (
  methods: Option[Seq[String]] = None,
  schemes: Option[Seq[String]] = None,
  url: String
)

case class RateLimitMatchResponse (
  status: Option[Seq[Int]] = None,
  originTraffic: Option[Boolean] = None,
  headers: Seq[RateLimitMatchResponseHeader]
)

case class RateLimitMatchResponseHeader (
  name: String,
  op: String,
  value: String
)