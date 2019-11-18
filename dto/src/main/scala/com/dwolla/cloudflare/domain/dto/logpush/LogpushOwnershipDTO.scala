package com.dwolla.cloudflare.domain.dto.logpush

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class LogpushOwnershipDTO(
  filename: String,
  message: String,
  valid: Boolean
)

object LogpushOwnershipDTO {
  implicit val logpushOwnershipDTOCodec: Codec[LogpushOwnershipDTO] = deriveCodec
}

case class CreateOwnershipDTO(destination_conf: String)

object CreateOwnershipDTO {
  implicit val createOwnershipDTOCodec: Codec[CreateOwnershipDTO] = deriveCodec
}
