package com.dwolla.cloudflare.domain.model.accounts

case class AccountMember (
  id: String,
  user: User,
  status: String,
  roles: List[AccountRole]
) {
  def uri(accountId: String): String = {
    s"https://api.cloudflare.com/client/v4/accounts/$accountId/members/$id"
  }
}
