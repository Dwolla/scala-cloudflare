package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountDTO (
  id: String,
  name: String,
  settings: AccountSettingsDTO
)

object AccountDTO {
  implicit val accountDTOCodec: Codec[AccountDTO] = deriveCodec
}
