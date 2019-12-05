package com.dwolla.circe

import io.circe.Decoder.Result
import io.circe.{ACursor, Decoder, DecodingFailure, FailedCursor, HCursor}

object NullAsEmptyListCodec extends NullAsEmptyListCodec

trait NullAsEmptyListCodec {
  implicit def listDecoder[A: Decoder]: Decoder[List[A]] = new Decoder[List[A]] {
    def apply(c: HCursor): Result[List[A]] = tryDecode(c)

    override def tryDecode(c: ACursor): Decoder.Result[List[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) Right(List.empty)
        else Decoder.decodeList[A].tryDecode(c)
      case c: FailedCursor =>
        if (!c.incorrectFocus) Right(List.empty) else Left(DecodingFailure("List[A]", c.history))
    }
  }
}
