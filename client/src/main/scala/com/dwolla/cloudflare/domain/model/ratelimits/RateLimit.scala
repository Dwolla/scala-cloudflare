package com.dwolla.cloudflare.domain.model.ratelimits

import com.dwolla.cloudflare.BaseUrl
import org.http4s.Uri

case class RateLimit (
  id: String,
  disabled: Option[Boolean] = None,
  description: Option[String] = None,
  trafficMatch: RateLimitMatch,
  bypass: Option[Seq[RateLimitKeyValue]] = None,
  threshold: Int,
  period: Int,
  action: RateLimitAction
) {
  def uri(zoneId: String): Uri =
    BaseUrl / "zones" / zoneId / "rate_limits" / id
}

case class CreateRateLimit (
  disabled: Option[Boolean] = None,
  description: Option[String] = None,
  trafficMatch: RateLimitMatch,
  bypass: Option[Seq[RateLimitKeyValue]] = None,
  threshold: Int,
  period: Int,
  action: RateLimitAction
)