package com.dwolla.cloudflare.domain.model.logpush

case class LogpushOwnership(
  filename: String,
  message: String,
  valid: Boolean
)


object LogpushOwnership {
  import io.circe.Codec
  import io.circe.generic.semiauto.deriveCodec
  implicit val codec: Codec[LogpushOwnership] = deriveCodec
}
