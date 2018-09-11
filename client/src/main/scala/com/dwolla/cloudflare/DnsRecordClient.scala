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
  def deleteDnsRecord(physicalResourceId: String): Stream[F, PhysicalResourceId]
  def getZoneId(domain: String): Stream[F, ZoneId]
}

object DnsRecordClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): DnsRecordClient[F] = new DnsRecordClientImpl[F](executor)
}

object DnsRecordClientImpl {
  val notFoundCodes = List(1032)
}

class DnsRecordClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends DnsRecordClient[F] with Http4sClientDsl[F] {

  import com.dwolla.cloudflare.domain.model.Implicits._

  override def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId ← getZoneId(domainNameToZoneName(record.name))
      request ← Stream.eval(POST(BaseUrl / "zones" / zoneId / "dns_records", record.toDto.asJson))
      record ← executor.fetch[DnsRecordDTO](request)
    } yield (record, zoneId)

  private def toUri(physicalResourceId: String): F[Uri] =
    Uri.fromString(physicalResourceId).fold(Sync[F].raiseError, Applicative[F].pure)

  override def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
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

  override def getExistingDnsRecord(physicalResourceId: String): Stream[F, IdentifiedDnsRecord] =
    for {
      uri ← Stream.eval(toUri(physicalResourceId))
      dto ← getExistingDnsRecordDto(uri).returningEmptyOnErrorCodes(7000, 7003)
    } yield dto.identifyAs(uri.toString())

  override def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord] = {
    for {
      zoneId ← getZoneId(domainNameToZoneName(name))
      record ← executor.fetch[DnsRecordDTO](Request[F](uri = BaseUrl / "zones" / zoneId / "dns_records" +?("name", name) +??("content", content) +??("type", recordType)))
    } yield (record, zoneId)
  }

  override def deleteDnsRecord(physicalResourceId: String): Stream[F, PhysicalResourceId] =
  /*_*/
    for {
      uri ← Stream.eval(toUri(physicalResourceId))
      req ← Stream.eval(DELETE(uri))
      json ← executor.fetch[Json](req).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.contains(Error(Option(1032), "Invalid DNS record identifier")) && ex.errors.length == 1 ⇒
          DnsRecordIdDoesNotExistException(physicalResourceId)
      }
    } yield tagPhysicalResourceId(json.flatMap(zoneIdLens).getOrElse(physicalResourceId))
  /*_*/

  private val zoneIdLens: Json ⇒ Option[String] = root.id.string.getOption

  override def getZoneId(domain: String): Stream[F, ZoneId] =
    executor.fetch[ZoneDTO](Request[F](uri = BaseUrl / "zones" +? ("name", domain) +? ("status", "active")))
      .map(_.id)
      .collect {
        case Some(id) ⇒ tagZoneId(id)
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
