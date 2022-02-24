package com.dwolla.cloudflare

import _root_.io.circe._
import _root_.io.circe.generic.auto._
import _root_.io.circe.optics.JsonPath._
import _root_.io.circe.syntax._
import _root_.org.http4s.circe._
import cats._
import cats.effect._
import cats.implicits._
import com.dwolla.cloudflare.DnsRecordClientImpl.{DnsRecordContent, DnsRecordName, DnsRecordType}
import com.dwolla.cloudflare.domain.dto.dns._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.{Error, _}
import fs2._
import monix.newtypes.NewtypeWrapped
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait DnsRecordClient[F[_]] {
  def getById(zoneId: ZoneId, resourceId: ResourceId): Stream[F, IdentifiedDnsRecord]
  def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord]
  def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord]
  def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord]
  def deleteDnsRecord(physicalResourceId: String): Stream[F, PhysicalResourceId]

  def getByUri(uri: String): Stream[F, IdentifiedDnsRecord] = parseUri(uri).fold(Stream.empty.covaryAll[F, IdentifiedDnsRecord]) {
    case (zoneId, resourceId) => getById(zoneId, resourceId)
  }

  def parseUri(uri: String): Option[(ZoneId, ResourceId)] = uri match {
    case DnsRecordClient.uriRegex(zoneId, resourceId) =>
      Option((tagZoneId(zoneId), tagResourceId(resourceId)))
    case _ =>
      None
  }
}

object DnsRecordClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): DnsRecordClient[F] = new DnsRecordClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/dns_records/(.+)""".r
}

object DnsRecordClientImpl {
  val notFoundCodes = List(1032)
  
  private[DnsRecordClientImpl] type DnsRecordName = DnsRecordName.Type
  private[DnsRecordClientImpl] object DnsRecordName extends NewtypeWrapped[String] {
    implicit val queryParam: QueryParam[DnsRecordName] = new QueryParam[DnsRecordName] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordName] = value => QueryParameterValue(value.value)
  }
  private[DnsRecordClientImpl] type DnsRecordContent = DnsRecordContent.Type
  private[DnsRecordClientImpl] object DnsRecordContent extends NewtypeWrapped[String] {
    implicit val queryParam: QueryParam[DnsRecordContent] = new QueryParam[DnsRecordContent] {
      override def key: QueryParameterKey = QueryParameterKey("content")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordContent] = value => QueryParameterValue(value.value)
  }
  private[DnsRecordClientImpl] type DnsRecordType = DnsRecordType.Type
  private[DnsRecordClientImpl] object DnsRecordType extends NewtypeWrapped[String] {
    implicit val queryParam: QueryParam[DnsRecordType] = new QueryParam[DnsRecordType] {
      override def key: QueryParameterKey = QueryParameterKey("type")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordType] = value => QueryParameterValue(value.value)
  }
}

class DnsRecordClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends DnsRecordClient[F] with Http4sClientDsl[F] {
  import com.dwolla.cloudflare.domain.model.Implicits._

  private val zoneClient = ZoneClient(executor)

  override def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId <- zoneClient.getZoneId(domainNameToZoneName(record.name))
      record <- executor.fetch[DnsRecordDTO](POST(record.toDto.asJson, BaseUrl / "zones" / zoneId / "dns_records"))
    } yield (record, zoneId)

  private def toUri(physicalResourceId: String): F[Uri] =
    Uri.fromString(physicalResourceId).fold(Sync[F].raiseError, Applicative[F].pure)

  override def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      uri <- Stream.emit(toUri(record.zoneId, record.resourceId)).covary[F]
      updatedRecord <- executor.fetch[DnsRecordDTO](PUT(record.unidentify.toDto.asJson, uri))
    } yield (updatedRecord, record.zoneId)

  private def toUri(zoneId: ZoneId, resourceId: ResourceId): Uri =
    BaseUrl / "zones" / zoneId / "dns_records" / resourceId

  private def getExistingDnsRecordDto(uri: Uri): Stream[F, DnsRecordDTO] =
    executor.fetch[DnsRecordDTO](GET(uri))

  override def getById(zoneId: ZoneId, resourceId: ResourceId): Stream[F, IdentifiedDnsRecord] =
    for {
      uri <- Stream.emit(toUri(zoneId, resourceId)).covary[F]
      dto <- getExistingDnsRecordDto(uri).returningEmptyOnErrorCodes(7000, 7003)
    } yield dto.identifyAs(uri.toString())

  override def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId <- zoneClient.getZoneId(domainNameToZoneName(name))
      record <- executor.fetch[DnsRecordDTO](Request[F](uri = BaseUrl / "zones" / zoneId / "dns_records" +*? DnsRecordName(name) +?? content.map(DnsRecordContent(_)) +?? recordType.map(DnsRecordType(_))))
    } yield (record, zoneId)

  override def deleteDnsRecord(physicalResourceId: String): Stream[F, PhysicalResourceId] =
    for {
      uri <- Stream.eval(toUri(physicalResourceId))
      json <- executor.fetch[Json](DELETE(uri)).last.adaptError {
        case ex: UnexpectedCloudflareErrorException if ex.errors.contains(Error(Option(1032), "Invalid DNS record identifier")) && ex.errors.length == 1 =>
          DnsRecordIdDoesNotExistException(physicalResourceId)
      }
    } yield tagPhysicalResourceId(json.flatMap(zoneIdLens).getOrElse(physicalResourceId))

  private val zoneIdLens: Json => Option[String] = root.id.string.getOption

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
