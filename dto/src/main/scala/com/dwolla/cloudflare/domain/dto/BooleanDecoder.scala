package com.dwolla.cloudflare.domain.dto

import io.circe.Decoder
import scala.util.Try

trait BooleanDecoder {
  protected implicit val stringBoolean: Decoder[Boolean] = Decoder.decodeBoolean.or(Decoder.decodeString.emapTry(s => Try(s.toBoolean)))
}
