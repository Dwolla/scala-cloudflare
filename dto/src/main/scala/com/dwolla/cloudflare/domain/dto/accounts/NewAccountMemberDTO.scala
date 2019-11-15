package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class NewAccountMemberDTO (
  email: String,
  roles: Seq[String],
  status: Option[String] = None
)

object NewAccountMemberDTO {
  implicit val newAccountMemberDTOEncoder: Encoder[NewAccountMemberDTO] = deriveEncoder
  implicit val newAccountMemberDTODecoder: Decoder[NewAccountMemberDTO] = deriveDecoder
}
