package com.dwolla.cloudflare.domain.model.ratelimits

import com.dwolla.cloudflare.BaseUrl
import com.dwolla.cloudflare.domain.model.{RateLimitId, ZoneId}
import org.http4s.Uri

case class RateLimit (
  id: RateLimitId,
  disabled: Option[Boolean] = None,
  description: Option[String] = None,
  trafficMatch: RateLimitMatch,
  bypass: Option[Seq[RateLimitKeyValue]] = None,
  threshold: Int,
  period: Int,
  action: RateLimitAction
) {
  def uri(zoneId: ZoneId): Uri =
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