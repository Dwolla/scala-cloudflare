package com.dwolla.cloudflare.domain.dto

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class ZoneSettingsDTO(id: String,
                           value: String,
                           editable: Option[Boolean],
                           modified_on: Option[String],
                          )

object ZoneSettingsDTO {
  implicit val zoneSettingsDTOEncoder: Encoder[ZoneSettingsDTO] = deriveEncoder
  implicit val zoneSettingsDTODecoder: Decoder[ZoneSettingsDTO] = deriveDecoder
}
