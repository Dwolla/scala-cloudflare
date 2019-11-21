package com.dwolla.cloudflare.domain.dto

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ZoneSettingsDTO(id: String,
                           value: String,
                           editable: Option[Boolean],
                           modified_on: Option[String],
                          )

object ZoneSettingsDTO {
  implicit val zoneSettingsDTOCodec: Codec[ZoneSettingsDTO] = deriveCodec
}
