package com.dwolla.cloudflare.domain.model.ratelimits

case class RateLimit (
  id: String,
  disabled: Option[Boolean] = None,
  description: Option[String] = None,
  trafficMatch: RateLimitMatch,
  bypass: Option[Seq[RateLimitKeyValue]] = None,
  threshold: Int,
  period: Int,
  action: RateLimitAction
)

case class CreateRateLimit (
  disabled: Option[Boolean] = None,
  description: Option[String] = None,
  trafficMatch: RateLimitMatch,
  bypass: Option[Seq[RateLimitKeyValue]] = None,
  threshold: Int,
  period: Int,
  action: RateLimitAction
)