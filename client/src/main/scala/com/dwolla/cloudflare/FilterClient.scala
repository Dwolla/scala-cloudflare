package com.dwolla.cloudflare

import cats.*
import cats.effect.{Trace as _, *}
import cats.syntax.all.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.filters.*
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.tagless.*
import com.dwolla.tracing.syntax.*
import io.circe.*
import io.circe.optics.JsonPath.*
import io.circe.syntax.*
import fs2.*
import natchez.Trace
import org.http4s.Method.*
import org.http4s.circe.*
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all.*
import org.http4s.{Request, Uri}

import scala.util.matching.Regex

trait FilterClient[F[_]] {
  def list(zoneId: ZoneId): F[Filter]
  def getById(zoneId: ZoneId, filterId: String): F[Filter]
  def create(zoneId: ZoneId, filter: Filter): F[Filter]
  def update(zoneId: ZoneId, filter: Filter): F[Filter]
  def delete(zoneId: ZoneId, filterId: String): F[FilterId]
  def getByUri(uri: String): F[Filter]

  def parseUri(uri: String): Option[(ZoneId, FilterId)] = uri match {
    case FilterClient.uriRegex(zoneId, filterId) => Option((tagZoneId(zoneId), tagFilterId(filterId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, filterId: FilterId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "filters" / filterId
}

object FilterClient extends FilterClientInstances {
  def apply[F[_] : MonadCancelThrow : Trace](executor: StreamingCloudflareApiExecutor[F]): FilterClient[Stream[F, *]] =
    apply(executor, _.traceWithInputsAndOutputs)

  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                     transform: FilterClient[Stream[F, *]] => FilterClient[Stream[F, *]]): FilterClient[Stream[F, *]] =
    WeaveKnot(knot(executor))(transform)

  private def knot[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): Eval[FilterClient[Stream[F, *]]] => FilterClient[Stream[F, *]] =
    new FilterClientImpl[F](executor, _)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/filters/(.+)""".r
}

private class FilterClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F],
                                                        self: Eval[FilterClient[Stream[F, *]]])
  extends FilterClient[Stream[F, *]]
    with Http4sClientDsl[F] {

  private def fetch(req: Request[F]): Stream[F, Filter] =
    executor.fetch[Filter](req)

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
      json <- executor.fetch[Json](DELETE(BaseUrl / "zones" / zoneId / "filters" / filterId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagFilterId(json.flatMap(deletedRecordLens).getOrElse(filterId))

  override def getByUri(uri: String): Stream[F, Filter] =
    parseUri(uri).fold(MonoidK[Stream[F, *]].empty[Filter]) {
      case (zoneId, FilterId(filterId)) => self.value.getById(zoneId, filterId)
    }

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1000)
}

case class CannotUpdateUnidentifiedFilter(filter: Filter) extends RuntimeException(s"Cannot update unidentified filter $filter")
