package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.BaseUrl
import com.dwolla.cloudflare.domain.model._
import org.http4s.Uri

case class AccountMember(id: AccountMemberId,
                         user: User,
                         status: String,
                         roles: Seq[AccountRole]
                        ) {
  def uri(accountId: AccountId): Uri =
    BaseUrl / "accounts" / accountId / "members" / id
}
