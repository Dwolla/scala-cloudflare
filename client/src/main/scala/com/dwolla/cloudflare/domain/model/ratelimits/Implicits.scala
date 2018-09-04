package com.dwolla.cloudflare.domain.model.ratelimits

import com.dwolla.cloudflare.domain.dto.ratelimits._

private[cloudflare] object Implicits {
  implicit def toModel(dto: RateLimitDTO): RateLimit = {
    RateLimit(
      id = dto.id.get,
      disabled = dto.disabled,
      description = dto.description,
      trafficMatch = dto.`match`,
      bypass = dto.bypass.map(kv ⇒ kv.map(toModel)),
      threshold = dto.threshold,
      period = dto.period,
      action = dto.action
    )
  }

  implicit def toModel(dto: RateLimitActionDTO): RateLimitAction = {
    dto match {
      case ChallengeRateLimitActionDTO ⇒ ChallengeRateLimitAction
      case JsChallengeRateLimitActionDTO ⇒ JsChallengeRateLimitAction
      case BanRateLimitActionDTO(timeout, response) ⇒ BanRateLimitAction(timeout, response.map(toModel))
      case SimulateRateLimitActionDTO(timeout, response) ⇒ SimulateRateLimitAction(timeout, response.map(toModel))
    }
  }

  implicit def toModel(dto: RateLimitActionResponseDTO): RateLimitActionResponse = {
    RateLimitActionResponse(
      contentType = dto.content_type,
      body = dto.body
    )
  }

  implicit def toModel(dto: RateLimitKeyValueDTO): RateLimitKeyValue = {
    RateLimitKeyValue(
      name = dto.name,
      value = dto.value
    )
  }

  implicit def toModel(dto: RateLimitMatchDTO): RateLimitMatch = {
    RateLimitMatch(
      request = dto.request,
      response = dto.response.map(toModel)
    )
  }

  implicit def toModel(dto: RateLimitMatchRequestDTO): RateLimitMatchRequest = {
    RateLimitMatchRequest(
      methods = dto.methods,
      schemes = dto.schemes,
      url = dto.url
    )
  }

  implicit def toModel(dto: RateLimitMatchResponseDTO): RateLimitMatchResponse = {
    RateLimitMatchResponse(
      status = dto.status,
      originTraffic = dto.origin_traffic,
      headers = dto.headers.map(toModel)
    )
  }

  implicit def toModel(dto: RateLimitMatchResponseHeaderDTO): RateLimitMatchResponseHeader = {
    RateLimitMatchResponseHeader(
      name = dto.name,
      op = dto.op,
      value = dto.value
    )
  }

  implicit def toDto(model: RateLimit): RateLimitDTO = {
    RateLimitDTO(
      id = Some(model.id),
      disabled = model.disabled,
      description = model.description,
      `match` = model.trafficMatch,
      bypass = model.bypass.map(kv ⇒ kv.map(toDto)),
      threshold = model.threshold,
      period = model.period,
      action = model.action
    )
  }

  implicit def toDto(model: CreateRateLimit): RateLimitDTO = {
    RateLimitDTO(
      id = None,
      disabled = model.disabled,
      description = model.description,
      `match` = model.trafficMatch,
      bypass = model.bypass.map(kv ⇒ kv.map(toDto)),
      threshold = model.threshold,
      period = model.period,
      action = model.action
    )
  }

  implicit def toDto(model: RateLimitAction): RateLimitActionDTO = {
    model match {
      case ChallengeRateLimitAction ⇒ ChallengeRateLimitActionDTO
      case JsChallengeRateLimitAction ⇒ JsChallengeRateLimitActionDTO
      case BanRateLimitAction(timeout, response) ⇒ BanRateLimitActionDTO(timeout, response.map(toDto))
      case SimulateRateLimitAction(timeout, response) ⇒ SimulateRateLimitActionDTO(timeout, response.map(toDto))
    }
  }

  implicit def toDto(model: RateLimitActionResponse): RateLimitActionResponseDTO = {
    RateLimitActionResponseDTO(
      content_type = model.contentType,
      body = model.body
    )
  }

  implicit def toDto(model: RateLimitKeyValue): RateLimitKeyValueDTO = {
    RateLimitKeyValueDTO(
      name = model.name,
      value = model.value
    )
  }

  implicit def toDto(model: RateLimitMatch): RateLimitMatchDTO = {
    RateLimitMatchDTO(
      request = model.request,
      response = model.response.map(toDto)
    )
  }

  implicit def toDto(model: RateLimitMatchRequest): RateLimitMatchRequestDTO = {
    RateLimitMatchRequestDTO(
      methods = model.methods,
      schemes = model.schemes,
      url = model.url
    )
  }

  implicit def toDto(model: RateLimitMatchResponse): RateLimitMatchResponseDTO = {
    RateLimitMatchResponseDTO(
      status = model.status,
      origin_traffic = model.originTraffic,
      headers = model.headers.map(toDto)
    )
  }

  implicit def toDto(model: RateLimitMatchResponseHeader): RateLimitMatchResponseHeaderDTO = {
    RateLimitMatchResponseHeaderDTO(
      name = model.name,
      op = model.op,
      value = model.value
    )
  }
}
