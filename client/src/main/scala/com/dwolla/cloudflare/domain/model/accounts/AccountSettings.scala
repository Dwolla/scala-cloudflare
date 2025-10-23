package com.dwolla.cloudflare.domain.model.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountSettings (
  enforceTwoFactor: Boolean
)

object AccountSettings {
  implicit val codec: Codec[AccountSettings] = deriveCodec
}
