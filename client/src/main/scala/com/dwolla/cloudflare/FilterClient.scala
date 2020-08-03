package com.dwolla.cloudflare

import cats.implicits._
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.cloudflare.domain.model.filters._
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import cats.effect.Sync
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method._
import org.http4s.Request
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl

import scala.util.matching.Regex

trait FilterClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, Filter]
  def getById(zoneId: ZoneId, filterId: String): Stream[F, Filter]
  def create(zoneId: ZoneId, filter: Filter): Stream[F, Filter]
  def update(zoneId: ZoneId, filter: Filter): Stream[F, Filter]
  def delete(zoneId: ZoneId, filterId: String): Stream[F, FilterId]

  def getByUri(uri: String): Stream[F, Filter] = parseUri(uri).fold(Stream.empty.covaryAll[F, Filter]) {
    case (zoneId, filterId) => getById(zoneId, filterId)
  }

  def parseUri(uri: String): Option[(ZoneId, FilterId)] = uri match {
    case FilterClient.uriRegex(zoneId, filterId) => Option((tagZoneId(zoneId), tagFilterId(filterId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, filterId: FilterId): String =
    s"https://api.cloudflare.com/client/v4/zones/$zoneId/filters/$filterId"

}

object FilterClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): FilterClient[F] = new FilterClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/filters/(.+)""".r
}

class FilterClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends FilterClient[F] with Http4sClientDsl[F] {
  private def fetch(reqF: F[Request[F]]): Stream[F, Filter] =
    for {
      req <- Stream.eval(reqF)
      res <- executor.fetch[Filter](req)
    } yield res

  override def list(zoneId: ZoneId): Stream[F, Filter] =
    fetch(GET(BaseUrl / "zones" / zoneId / "filters"))

  override def getById(zoneId: ZoneId, filterId: String): Stream[F, Filter] =
    fetch(GET(BaseUrl / "zones" / zoneId / "filters" / filterId))

  override def create(zoneId: ZoneId, filter: Filter): Stream[F, Filter] =
    fetch(POST(List(filter).asJson, BaseUrl / "zones" / zoneId / "filters"))

  override def update(zoneId: ZoneId, filter: Filter): Stream[F, Filter] =
    // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (filter.id.isDefined)
      fetch(PUT(filter.copy(id = None).asJson, BaseUrl / "zones" / zoneId / "filters" / filter.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedFilter(filter))

  override def delete(zoneId: ZoneId, filterId: String): Stream[F, FilterId] =
    for {
      req <- Stream.eval(DELETE(BaseUrl / "zones" / zoneId / "filters" / filterId))
      json <- executor.fetch[Json](req).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagFilterId(json.flatMap(deletedRecordLens).getOrElse(filterId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1000)
}

case class CannotUpdateUnidentifiedFilter(filter: Filter) extends RuntimeException(s"Cannot update unidentified filter $filter")
