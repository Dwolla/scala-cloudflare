package com.dwolla.cloudflare

import _root_.io.circe._
import _root_.io.circe.generic.auto._
import _root_.io.circe.optics.JsonPath._
import _root_.io.circe.syntax._
import _root_.org.http4s.circe._
import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.domain.dto.dns._
import com.dwolla.cloudflare.domain.model
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.{Error, _}
import fs2._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.Http4sClientDsl

import scala.language.higherKinds

trait DnsRecordClient[F[_]] {
  def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord]
  def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord]
  def getExistingDnsRecord(physicalResourceId: String): Stream[F, IdentifiedDnsRecord]
  def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord]
  def deleteDnsRecord(physicalResourceId: String): Stream[F, String]
  def getZoneId(domain: String): Stream[F, String]
}

object DnsRecordClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): DnsRecordClient[F] = new DnsRecordClientImpl[F](executor)
}

class DnsRecordClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends DnsRecordClient[F] with Http4sClientDsl[F] {

  import com.dwolla.cloudflare.domain.model.Implicits._

  def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId ← getZoneId(domainNameToZoneName(record.name))
      request ← Stream.eval(POST(cloudflareBaseUri / "zones" / zoneId / "dns_records", record.toDto.asJson))
      record ← executor.fetch[DnsRecordDTO](request)
    } yield (record, zoneId)

  private def toUri(physicalResourceId: String): F[Uri] =
    Uri.fromString(physicalResourceId).fold(Sync[F].raiseError, Applicative[F].pure)

  def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      uri ← Stream.eval(toUri(record.physicalResourceId))
      req ← Stream.eval(PUT(uri, record.unidentify.toDto.asJson))
      updatedRecord ← executor.fetch[DnsRecordDTO](req)
    } yield (updatedRecord, record.zoneId)

  private def getExistingDnsRecordDto(uri: Uri): Stream[F, DnsRecordDTO] =
    for {
      req ← Stream.eval(GET(uri))
      res ← executor.fetch[DnsRecordDTO](req)
    } yield res

  def getExistingDnsRecord(physicalResourceId: String): Stream[F, IdentifiedDnsRecord] =
    for {
      uri ← Stream.eval(toUri(physicalResourceId))
      dto ← getExistingDnsRecordDto(uri)
    } yield dto.identifyAs(uri.toString())

  def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord] = {
    for {
      zoneId ← getZoneId(domainNameToZoneName(name))
      record ← executor.fetch[DnsRecordDTO](Request[F](uri = cloudflareBaseUri / "zones" / zoneId / "dns_records" +?("name", name) +??("content", content) +??("type", recordType)))
    } yield (record, zoneId)
  }

  private def handleDeleteResponseJson(json: Json, status: Status, physicalResourceId: String): F[String] =
    if (status.isSuccess)
      zoneIdLens(json).fold(Applicative[F].pure(physicalResourceId))(Applicative[F].pure)
    else {
      val errors = errorsLens(json)

      if (status == Status.BadRequest && errors.contains(Error(1032, "Invalid DNS record identifier")) && errors.length == 1)
        Sync[F].raiseError(DnsRecordIdDoesNotExistException(physicalResourceId))
      else
        Sync[F].raiseError(UnexpectedCloudflareErrorException(errors))
    }

  private def deleteDnsRecordF(physicalResourceId: String): F[String] =
    for {
      uri ← toUri(physicalResourceId)
      req ← DELETE(uri)
      id ← executor.raw(req) { res ⇒
        for {
          json ← res.decodeJson[Json]
          output ← handleDeleteResponseJson(json, res.status, physicalResourceId)
        } yield output
      }
    } yield id

  def deleteDnsRecord(physicalResourceId: String): Stream[F, String] = Stream.eval(deleteDnsRecordF(physicalResourceId))

  private val zoneIdLens: Json ⇒ Option[String] = root.result.id.string.getOption
  private val errorsLens: Json ⇒ List[Error] = root.errors.each.as[model.Error].getAll

  private val cloudflareBaseUri = Uri.uri("https://api.cloudflare.com") / "client" / "v4"

  def getZoneId(domain: String): Stream[F, String] =
    executor.fetch[ZoneDTO](Request[F](uri = cloudflareBaseUri / "zones" +? ("name", domain) +? ("status", "active")))
      .map(_.id)
      .collect {
        case Some(id) ⇒ id
      }

  private def domainNameToZoneName(name: String): String = name.split('.').takeRight(2).mkString(".")
}

case class MultipleCloudflareRecordsExistForDomainNameException(domainName: String, records: Set[DnsRecordDTO]) extends RuntimeException(
  s"""Multiple DNS records exist for domain name $domainName:
     |
     | - ${records.mkString("\n - ")}
     |
     |This resource refuses to process multiple records because the intention is not clear.
     |Clean up the records manually or provide additional parameters to filter on.""".stripMargin)

case class DnsRecordIdDoesNotExistException(resourceId: String) extends RuntimeException(
  s"The given DNS record ID does not exist ($resourceId).")
