package com.dwolla.circe

import cats.syntax.all.*
import io.circe.*

object StringAsBooleanCodec extends StringAsBooleanCodec

trait StringAsBooleanCodec {
  implicit val booleanDecoder: Decoder[Boolean] = Decoder.decodeBoolean or Decoder.decodeString.emap {
    case s if s.toLowerCase() == "true" => true.asRight
    case s if s.toLowerCase() == "false" => false.asRight
    case _ => "Boolean".asLeft
  }
}
