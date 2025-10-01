package com.dwolla.circe

import io.circe.{Decoder, Encoder}

import java.time.Duration

object DurationAsSecondsCodec extends DurationAsSecondsCodec

trait DurationAsSecondsCodec {
  implicit val durationEncoder: Encoder[Duration] = Encoder[Long].contramap(_.getSeconds)
  implicit val durationDecoder: Decoder[Duration] = Decoder[Long].map(Duration.ofSeconds)
}
