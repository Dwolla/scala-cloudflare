package com.dwolla.cloudflare.domain.model.logpush

import java.time.Instant

import com.dwolla.cloudflare.domain.model.{LogpullOptions, LogpushDestination, LogpushId}

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

case class CreateJob(
  destinationConf: LogpushDestination,
  ownershipChallenge: String,
  name: Option[String],
  enabled: Option[Boolean],
  logpullOptions: Option[LogpullOptions]
)
