package com.dwolla.cloudflare
package domain.model.accounts

import com.dwolla.cloudflare.domain.model.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.Uri

case class AccountMember(id: AccountMemberId,
                         user: User,
                         status: String,
                         roles: Seq[AccountRole]
                        ) {
  def uri(accountId: AccountId): Uri =
    BaseUrl / "accounts" / accountId / "members" / id
}

object AccountMember {
  implicit val codec: Codec[AccountMember] = deriveCodec
}
