package com.dwolla.cloudflare.domain.dto.logpush

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class LogpushJobDTO(
  id: Int,
  enabled: Boolean,
  name: Option[String],
  logpull_options: Option[String],
  destination_conf: String,
  last_complete: Option[String],
  last_error: Option[String],
  error_message: Option[String]
)

object LogpushJobDTO {
  implicit val logpushJobDTOCodec: Codec[LogpushJobDTO] = deriveCodec
}

case class CreateJobDTO(
  destination_conf: String,
  ownership_challenge: String,
  name: Option[String],
  enabled: Option[Boolean],
  logpull_options: Option[String]
)

object CreateJobDTO {
  implicit val createJobDTOCodec: Codec[CreateJobDTO] = deriveCodec
}
