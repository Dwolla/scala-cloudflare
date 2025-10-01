package com.dwolla.circe

import cats.syntax.all.*
import io.circe.Decoder.{AccumulatingResult, Result}
import io.circe.*

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
      case _ => super.tryDecode(c)
    }

    override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[List[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) List.empty.validNel
        else Decoder.decodeList[A].tryDecodeAccumulating(c)
      case c: FailedCursor =>
        if (!c.incorrectFocus) List.empty.validNel else DecodingFailure("List[A]", c.history).invalidNel
      case _ => super.tryDecodeAccumulating(c)
    }

  }
}
