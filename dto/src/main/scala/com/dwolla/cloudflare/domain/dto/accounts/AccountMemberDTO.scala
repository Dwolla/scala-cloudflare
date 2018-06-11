package com.dwolla.cloudflare.domain.dto.accounts

import com.dwolla.cloudflare.domain.dto.JsonWritable

case class AccountMemberDTO (
  id: String,
  user: UserDTO,
  status: String,
  roles: List[AccountRoleDTO]
) extends JsonWritable
