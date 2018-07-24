package com.dwolla.cloudflare.domain.dto.accounts

case class NewAccountMemberDTO (
  email: String,
  roles: Seq[String],
  status: Option[String] = None
)
