package com.dwolla.cloudflare.domain.model.accounts

case class AccountMember (
  id: String,
  user: User,
  status: String,
  roles: Seq[AccountRole]
) {
  def uri(accountId: String): String = {
    s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$id"
  }
}
