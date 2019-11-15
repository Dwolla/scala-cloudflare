package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class AccountMemberDTO (
  id: String,
  user: UserDTO,
  status: String,
  roles: Seq[AccountRoleDTO]
)

object AccountMemberDTO {
  implicit val accountMemberDTOEncoder: Encoder[AccountMemberDTO] = deriveEncoder
  implicit val accountMemberDTODecoder: Decoder[AccountMemberDTO] = deriveDecoder
}
