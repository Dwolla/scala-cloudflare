package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class AccountSettingsDTO (
  enforce_twofactor: Boolean
)

object AccountSettingsDTO {
  implicit val accountSettingsDTOEncoder: Encoder[AccountSettingsDTO] = deriveEncoder
  implicit val accountSettingsDTODecoder: Decoder[AccountSettingsDTO] = deriveDecoder
}
