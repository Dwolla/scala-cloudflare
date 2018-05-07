package com.dwolla.cloudflare.domain.model.accounts

case class AccountMember (
  id: String,
  user: User,
  status: String,
  roles: List[AccountRole]
)
