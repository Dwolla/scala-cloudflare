package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import cats.tagless.aop.Aspect
import com.dwolla.cloudflare.DnsRecordClientImpl.{DnsRecordContent, DnsRecordName, DnsRecordType}
import com.dwolla.cloudflare.domain.dto.dns.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.tagless.*
import com.dwolla.tracing.TraceWeaveCapturingInputsAndOutputs
import io.circe.optics.JsonPath.*
import io.circe.syntax.*
import io.circe.{Error as _, *}
import fs2.*
import natchez.*
import org.http4s.*
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait DnsRecordClient[F[_]] {
  def getById(zoneId: ZoneId, resourceId: ResourceId): F[IdentifiedDnsRecord]
  def createDnsRecord(record: UnidentifiedDnsRecord): F[IdentifiedDnsRecord]
  def updateDnsRecord(record: IdentifiedDnsRecord): F[IdentifiedDnsRecord]
  def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): F[IdentifiedDnsRecord]
  def deleteDnsRecord(physicalResourceId: String): F[PhysicalResourceId]
  def getByUri(uri: String): F[IdentifiedDnsRecord]

  def parseUri(uri: String): Option[(ZoneId, ResourceId)] = uri match {
    case DnsRecordClient.uriRegex(zoneId, resourceId) =>
      Option((tagZoneId(zoneId), tagResourceId(resourceId)))
    case _ =>
      None
  }
}

object DnsRecordClient extends DnsRecordClientInstances {
  def apply[F[_] : Concurrent : Trace](executor: StreamingCloudflareApiExecutor[F]): DnsRecordClient[Stream[F, *]] =
    apply(executor, new TraceWeaveCapturingInputsAndOutputs)

  def apply[F[_] : Concurrent, Dom[_], Cod[_]](executor: StreamingCloudflareApiExecutor[F],
                                               transform: Aspect.Weave[Stream[F, *], Dom, Cod, *] ~> Stream[F, *])
                                              (implicit A: Aspect[DnsRecordClient, Dom, Cod],
                                               A2: Aspect[ZoneClient, Dom, Cod]): DnsRecordClient[Stream[F, *]] =
    WeaveKnot.weave(knot(executor, transform), transform)

  private def knot[F[_] : Concurrent, Dom[_], Cod[_]](executor: StreamingCloudflareApiExecutor[F],
                                                      transform: Aspect.Weave[Stream[F, *], Dom, Cod, *] ~> Stream[F, *])
                                                     (implicit A: Aspect[ZoneClient, Dom, Cod]): Eval[DnsRecordClient[Stream[F, *]]] => DnsRecordClient[Stream[F, *]] =
    new DnsRecordClientImpl[F](executor, _, ZoneClient(executor, transform))

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/dns_records/(.+)""".r
}

private object DnsRecordClientImpl {
  val notFoundCodes = List(1032)

  private[DnsRecordClientImpl] type DnsRecordName = DnsRecordName.Type
  private[DnsRecordClientImpl] object DnsRecordName extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[DnsRecordName] = new QueryParam[DnsRecordName] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordName] = value => QueryParameterValue(value.value)
  }
  private[DnsRecordClientImpl] type DnsRecordContent = DnsRecordContent.Type
  private[DnsRecordClientImpl] object DnsRecordContent extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[DnsRecordContent] = new QueryParam[DnsRecordContent] {
      override def key: QueryParameterKey = QueryParameterKey("content")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordContent] = value => QueryParameterValue(value.value)
  }
  private[DnsRecordClientImpl] type DnsRecordType = DnsRecordType.Type
  private[DnsRecordClientImpl] object DnsRecordType extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[DnsRecordType] = new QueryParam[DnsRecordType] {
      override def key: QueryParameterKey = QueryParameterKey("type")
    }
    implicit val queryParamEncoder: QueryParamEncoder[DnsRecordType] = value => QueryParameterValue(value.value)
  }
}

private class DnsRecordClientImpl[F[_] : Concurrent](executor: StreamingCloudflareApiExecutor[F],
                                                     self: Eval[DnsRecordClient[Stream[F, *]]],
                                                     zoneClient: ZoneClient[Stream[F, *]])
  extends DnsRecordClient[Stream[F, *]]
    with Http4sClientDsl[F] {
  import com.dwolla.cloudflare.domain.model.Implicits.*

  override def getByUri(uri: String): Stream[F, IdentifiedDnsRecord] =
    self.value.parseUri(uri)
      .fold(MonoidK[Stream[F, *]].empty[IdentifiedDnsRecord]) {
        case (zoneId, resourceId) => self.value.getById(zoneId, resourceId)
      }

  override def createDnsRecord(record: UnidentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId <- zoneClient.getZoneId(domainNameToZoneName(record.name))
      record <- executor.fetch[DnsRecordDTO](POST(record.toDto.asJson, BaseUrl / "zones" / zoneId / "dns_records"))
      out <- Stream.emits(fromDtoZoneId(record, zoneId).toSeq)
    } yield out

  private def toUri(physicalResourceId: String): F[Uri] =
    Uri.fromString(physicalResourceId).liftTo[F]

  override def updateDnsRecord(record: IdentifiedDnsRecord): Stream[F, IdentifiedDnsRecord] =
    for {
      uri <- Stream.emit(toUri(record.zoneId, record.resourceId)).covary[F]
      updatedRecord <- executor.fetch[DnsRecordDTO](PUT(record.unidentify.toDto.asJson, uri))
      out <- Stream.emits(fromDtoZoneId(updatedRecord, record.zoneId).toSeq)
    } yield out

  private def toUri(zoneId: ZoneId, resourceId: ResourceId): Uri =
    BaseUrl / "zones" / zoneId / "dns_records" / resourceId

  private def getExistingDnsRecordDto(uri: Uri): Stream[F, DnsRecordDTO] =
    executor.fetch[DnsRecordDTO](GET(uri))

  override def getById(zoneId: ZoneId, resourceId: ResourceId): Stream[F, IdentifiedDnsRecord] =
    for {
      uri <- Stream.emit(toUri(zoneId, resourceId)).covary[F]
      dto <- getExistingDnsRecordDto(uri).returningEmptyOnErrorCodes(7000, 7003)
    } yield fromDto(dto).identifyAs(uri.toString())

  override def getExistingDnsRecords(name: String, content: Option[String] = None, recordType: Option[String] = None): Stream[F, IdentifiedDnsRecord] =
    for {
      zoneId <- zoneClient.getZoneId(domainNameToZoneName(name))
      record <- executor.fetch[DnsRecordDTO](Request[F](uri = BaseUrl / "zones" / zoneId / "dns_records" +*? DnsRecordName(name) +?? content.map(DnsRecordContent(_)) +?? recordType.map(DnsRecordType(_))))
      out <- Stream.emits(fromDtoZoneId(record, zoneId).toSeq)
    } yield out

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
