package com.dwolla.cloudflare.domain.dto.ratelimits

sealed trait RateLimitActionDTO {
  val mode: String
  val response: Option[RateLimitActionResponseDTO]
}

sealed trait TimeoutRateLimitActionDTO extends RateLimitActionDTO {
  val timeout: Int
}

case class BanRateLimitActionDTO(timeout: Int, response: Option[RateLimitActionResponseDTO] = None) extends TimeoutRateLimitActionDTO {
  override val mode = "ban"
}

case class SimulateRateLimitActionDTO(timeout: Int, response: Option[RateLimitActionResponseDTO] = None) extends TimeoutRateLimitActionDTO {
  override val mode = "simulate"
}

case object ChallengeRateLimitActionDTO extends RateLimitActionDTO {
  override val mode = "challenge"
  override val response: Option[RateLimitActionResponseDTO] = None
}

case object JsChallengeRateLimitActionDTO extends RateLimitActionDTO {
  override val mode = "js_challenge"
  override val response: Option[RateLimitActionResponseDTO] = None
}

case class RateLimitActionResponseDTO (
  content_type: String,
  body: String
)
