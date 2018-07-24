package com.dwolla.cloudflare.domain.dto.accounts

case class AccountMemberDTO (
  id: String,
  user: UserDTO,
  status: String,
  roles: Seq[AccountRoleDTO]
)
