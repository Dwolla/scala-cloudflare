package com.dwolla.cloudflare.domain.dto.logpush

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class LogpushOwnershipDTO(
  filename: String,
  message: String,
  valid: Boolean
)

object LogpushOwnershipDTO {
  implicit val logpushOwnershipDTOEncoder: Encoder[LogpushOwnershipDTO] = deriveEncoder
  implicit val logpushOwnershipDTODecoder: Decoder[LogpushOwnershipDTO] = deriveDecoder
}

case class CreateOwnershipDTO(destination_conf: String)

object CreateOwnershipDTO {
  implicit val createOwnershipDTOEncoder: Encoder[CreateOwnershipDTO] = deriveEncoder
  implicit val createOwnershipDTODecoder: Decoder[CreateOwnershipDTO] = deriveDecoder
}
