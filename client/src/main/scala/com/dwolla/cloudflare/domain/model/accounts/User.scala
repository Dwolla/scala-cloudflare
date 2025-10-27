package com.dwolla.cloudflare.domain.model.accounts

import com.dwolla.cloudflare.domain.model.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class User(id: UserId,
                firstName: Option[String],
                lastName: Option[String],
                emailAddress: String,
                twoFactorEnabled: Boolean
               )

object User {
  implicit val userCodec: Codec[User] = deriveCodec
}
