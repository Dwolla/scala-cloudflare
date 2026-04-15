package com.dwolla.cloudflare.domain.dto

import com.dwolla.cloudflare.domain.dto.BooleanDecoder
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ZoneSettingsDTO(id: String,
                           value: String,
                           editable: Option[Boolean],
                           modified_on: Option[String],
                          )

object ZoneSettingsDTO extends BooleanDecoder {
  implicit val zoneSettingsDTOCodec: Codec[ZoneSettingsDTO] = deriveCodec
}
