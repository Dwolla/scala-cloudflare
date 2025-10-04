package com.dwolla.cloudflare.domain.model.logpush

import com.dwolla.cloudflare.domain.model.{LogpullOptions, LogpushDestination, LogpushId}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.time.Instant

case class LogpushJob(
  id: LogpushId,
  enabled: Boolean,
  name: Option[String],
  logpullOptions: Option[LogpullOptions],
  destinationConf: LogpushDestination,
  lastComplete: Option[Instant],
  lastError: Option[Instant],
  errorMessage: Option[String]
)

object LogpushJob {
  implicit val codec: Codec[LogpushJob] = deriveCodec
}

case class CreateJob(
  destinationConf: LogpushDestination,
  ownershipChallenge: String,
  name: Option[String],
  enabled: Option[Boolean],
  logpullOptions: Option[LogpullOptions]
)

object CreateJob {
  implicit val codec: Codec[CreateJob] = deriveCodec
}
