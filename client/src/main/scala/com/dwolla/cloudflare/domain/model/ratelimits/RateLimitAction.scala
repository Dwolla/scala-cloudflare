package com.dwolla.cloudflare.domain.model.ratelimits

sealed trait RateLimitAction {
  val mode: String
  val response: Option[RateLimitActionResponse]
}

sealed trait TimeoutRateLimitAction extends RateLimitAction {
  val timeout: Int
}

case class BanRateLimitAction(timeout: Int, response: Option[RateLimitActionResponse] = None) extends TimeoutRateLimitAction {
  override val mode = "ban"
}

case class SimulateRateLimitAction(timeout: Int, response: Option[RateLimitActionResponse] = None) extends TimeoutRateLimitAction {
  override val mode = "simulate"
}

case object ChallengeRateLimitAction extends RateLimitAction {
  override val mode = "challenge"
  override val response: Option[RateLimitActionResponse] = None
}

case object JsChallengeRateLimitAction extends RateLimitAction {
  override val mode = "js_challenge"
  override val response: Option[RateLimitActionResponse] = None
}

case class RateLimitActionResponse (
  contentType: String,
  body: String
)
