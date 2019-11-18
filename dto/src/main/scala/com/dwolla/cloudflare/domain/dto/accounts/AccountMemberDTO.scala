package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountMemberDTO (
  id: String,
  user: UserDTO,
  status: String,
  roles: Seq[AccountRoleDTO]
)

object AccountMemberDTO {
  implicit val accountMemberDTOCodec: Codec[AccountMemberDTO] = deriveCodec
}
