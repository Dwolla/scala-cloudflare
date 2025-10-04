package com.dwolla.cloudflare

import cats.effect.{Trace as _, *}
import com.dwolla.cloudflare.domain.dto.logpush.{CreateJobDTO, CreateOwnershipDTO, LogpushJobDTO, LogpushOwnershipDTO}
import com.dwolla.cloudflare.domain.model.logpush.*
import com.dwolla.cloudflare.domain.model.{Implicits as _, *}
import com.dwolla.tracing.syntax.*
import io.circe.syntax.*
import fs2.*
import natchez.Trace
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl

import java.time.Instant

trait LogpushClient[F[_]] {
  def list(zoneId: ZoneId): F[LogpushJob]
  def createOwnership(zoneId: ZoneId, destination: LogpushDestination): F[LogpushOwnership]
  def createJob(zoneId: ZoneId, job: CreateJob): F[LogpushJob]
}

object LogpushClient extends LogpushClientInstances {
  def apply[F[_] : MonadCancelThrow : Trace](executor: StreamingCloudflareApiExecutor[F]): LogpushClient[Stream[F, *]] =
    (new LogpushClientImpl(executor): LogpushClient[Stream[F, *]]).traceWithInputsAndOutputs
}

private class LogpushClientImpl[F[_]](executor: StreamingCloudflareApiExecutor[F])
  extends LogpushClient[Stream[F, *]] with Http4sClientDsl[F] {

  override def list(zoneId: ZoneId): Stream[F, LogpushJob] =
    executor.fetch[LogpushJobDTO](GET(BaseUrl / "zones" / zoneId / "logpush" / "jobs"))
      .map(toModel)

  override def createOwnership(zoneId: ZoneId, destination: LogpushDestination): Stream[F, LogpushOwnership] =
    executor.fetch[LogpushOwnershipDTO](POST(toDto(destination).asJson, BaseUrl / "zones" / zoneId / "logpush" / "ownership"))
      .map(toModel)

  override def createJob(zoneId: ZoneId, job: CreateJob): Stream[F, LogpushJob] =
    executor.fetch[LogpushJobDTO](POST(toDto(job).asJson, BaseUrl / "zones" / zoneId / "logpush" / "jobs"))
      .map(toModel)

  private def toModel(dto: LogpushJobDTO) =
    LogpushJob(
      id = tagLogpushId(dto.id),
      enabled = dto.enabled,
      name = dto.name,
      logpullOptions = dto.logpull_options.map(opts => tagLogpullOptions(opts)),
      destinationConf = tagLogpushDestination(dto.destination_conf),
      lastComplete = dto.last_complete.map(ts => Instant.parse(ts)),
      lastError = dto.last_error.map(ts => Instant.parse(ts)),
      errorMessage = dto.error_message
    )

  private def toModel(dto: LogpushOwnershipDTO) =
    LogpushOwnership(
      filename = dto.filename,
      message = dto.message,
      valid = dto.valid
    )

  private def toDto(destination: LogpushDestination) = CreateOwnershipDTO(destination.value)

  private def toDto(model: CreateJob) =
    CreateJobDTO(
      destination_conf = model.destinationConf.value,
      ownership_challenge = model.ownershipChallenge,
      name = model.name,
      enabled = model.enabled,
      logpull_options = model.logpullOptions.map(_.value)
    )
}
