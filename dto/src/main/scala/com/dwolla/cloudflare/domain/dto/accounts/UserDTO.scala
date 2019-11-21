package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class UserDTO (
  id: String,
  first_name: Option[String],
  last_name: Option[String],
  email: String,
  two_factor_authentication_enabled: Boolean
)

object UserDTO {
  implicit val userDTOCodec: Codec[UserDTO] = deriveCodec
}
