package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class UserDTO (
  id: String,
  first_name: Option[String],
  last_name: Option[String],
  email: String,
  two_factor_authentication_enabled: Boolean
)

object UserDTO {
  implicit val userDTOEncoder: Encoder[UserDTO] = deriveEncoder
  implicit val userDTODecoder: Decoder[UserDTO] = deriveDecoder
}
