package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class AccountDTO (
  id: String,
  name: String,
  settings: AccountSettingsDTO
)

object AccountDTO {
  implicit val accountDTOEncoder: Encoder[AccountDTO] = deriveEncoder
  implicit val accountDTODecoder: Decoder[AccountDTO] = deriveDecoder
}
