package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class NewAccountMemberDTO (
  email: String,
  roles: Seq[String],
  status: Option[String] = None
)

object NewAccountMemberDTO {
  implicit val newAccountMemberDTOCodec: Codec[NewAccountMemberDTO] = deriveCodec
}
