package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.BaseUrl
import org.http4s.Uri

case class AccountMember (
  id: String,
  user: User,
  status: String,
  roles: Seq[AccountRole]
) {
  def uri(accountId: String): Uri =
    BaseUrl / "accounts" / accountId / "members" / id
}
