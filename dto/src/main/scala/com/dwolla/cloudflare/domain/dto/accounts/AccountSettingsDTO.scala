package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountSettingsDTO (
  enforce_twofactor: Boolean
)

object AccountSettingsDTO {
  implicit val accountSettingsDTOCodec: Codec[AccountSettingsDTO] = deriveCodec
}
