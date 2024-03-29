package com.dwolla.cloudflare

import com.dwolla.cloudflare.domain.dto.logpush.{CreateJobDTO, CreateOwnershipDTO, LogpushJobDTO, LogpushOwnershipDTO}
import com.dwolla.cloudflare.domain.model.logpush._
import com.dwolla.cloudflare.domain.model.{Implicits => _, _}
import io.circe.syntax._
import fs2._
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import java.time.Instant

trait LogpushClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, LogpushJob]
  def createOwnership(zoneId: ZoneId, destination: LogpushDestination): Stream[F, LogpushOwnership]
  def createJob(zoneId: ZoneId, job: CreateJob): Stream[F, LogpushJob]
}

object LogpushClient {
  def apply[F[_]](executor: StreamingCloudflareApiExecutor[F]): LogpushClient[F] = new LogpushClientImpl(executor)
}

class LogpushClientImpl[F[_]](executor: StreamingCloudflareApiExecutor[F]) extends LogpushClient[F] with Http4sClientDsl[F] {
  override def list(zoneId: ZoneId): Stream[F, LogpushJob] =
    for {
      res <- executor.fetch[LogpushJobDTO](GET(BaseUrl / "zones" / zoneId / "logpush" / "jobs"))
    } yield toModel(res)

  override def createOwnership(zoneId: ZoneId, destination: LogpushDestination): Stream[F, LogpushOwnership] =
    for {
      res <- executor.fetch[LogpushOwnershipDTO](POST(toDto(destination).asJson, BaseUrl / "zones" / zoneId / "logpush" / "ownership"))
    } yield toModel(res)

  override def createJob(zoneId: ZoneId, job: CreateJob): Stream[F, LogpushJob] =
    for {
      res <- executor.fetch[LogpushJobDTO](POST(toDto(job).asJson, BaseUrl / "zones" / zoneId / "logpush" / "jobs"))
    } yield toModel(res)

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

  private def toDto(destination: LogpushDestination) = CreateOwnershipDTO(destination)

  private def toDto(model: CreateJob) =
    CreateJobDTO(
      destination_conf = model.destinationConf,
      ownership_challenge = model.ownershipChallenge,
      name = model.name,
      enabled = model.enabled,
      logpull_options = model.logpullOptions
    )
}
