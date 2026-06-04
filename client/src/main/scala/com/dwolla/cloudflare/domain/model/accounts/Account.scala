package com.dwolla.cloudflare
package domain
package model
package accounts

import com.dwolla.cloudflare.domain.model.AccountId
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Account(id: AccountId,
                   name: String,
                   settings: AccountSettings
                  )

object Account {
  implicit val codec: Codec[Account] = deriveCodec
}
