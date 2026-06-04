package com.dwolla

import io.circe.Decoder
import scala.util.Try

package object cloudflare {
  private[cloudflare] implicit val stringBoolean: Decoder[Boolean] =
    Decoder.decodeBoolean.or(Decoder.decodeString.emapTry(s => Try(s.toBoolean)))
}
