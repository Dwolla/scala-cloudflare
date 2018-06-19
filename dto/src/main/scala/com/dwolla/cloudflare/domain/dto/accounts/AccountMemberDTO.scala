package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class AccountMemberDTO (
  id: String,
  user: UserDTO,
  status: String,
  roles: Seq[AccountRoleDTO]
) extends JsonWritable
