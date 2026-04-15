package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.BooleanDecoder
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountSettingsDTO (
  enforce_twofactor: Boolean
)

object AccountSettingsDTO extends BooleanDecoder {
  implicit val accountSettingsDTOCodec: Codec[AccountSettingsDTO] = deriveCodec
}
