package com.dwolla.cloudflare

import cats._
import cats.syntax.all._
import com.dwolla.cloudflare.domain.model.{ZoneId, tagZoneId}
import com.dwolla.cloudflare.domain.model.pagerules._
import io.circe.syntax._
import io.circe._
import io.circe.optics.JsonPath._
import fs2._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import org.http4s.Method._
import org.http4s.{Request, Uri}
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.syntax.all._

import scala.util.matching.Regex

trait PageRuleClient[F[_]] {
  def list(zoneId: ZoneId): Stream[F, PageRule]
  def getById(zoneId: ZoneId, pageRuleId: String): Stream[F, PageRule]
  def create(zoneId: ZoneId, pageRule: PageRule): Stream[F, PageRule]
  def update(zoneId: ZoneId, pageRule: PageRule): Stream[F, PageRule]
  def delete(zoneId: ZoneId, pageRuleId: String): Stream[F, PageRuleId]

  def getByUri(uri: String): Stream[F, PageRule] = parseUri(uri).fold(Stream.empty.covaryAll[F, PageRule]) {
    case (zoneId, PageRuleId(pageRuleId)) => getById(zoneId, pageRuleId)
  }

  def parseUri(uri: String): Option[(ZoneId, PageRuleId)] = uri match {
    case PageRuleClient.uriRegex(zoneId, pageRuleId) => Option((tagZoneId(zoneId), tagPageRuleId(pageRuleId)))
    case _ => None
  }

  def buildUri(zoneId: ZoneId, pageRuleId: PageRuleId): Uri =
    uri"https://api.cloudflare.com/client/v4/zones" / zoneId / "pagerules" / pageRuleId

}

object PageRuleClient {
  def apply[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]): PageRuleClient[F] = new PageRuleClientImpl[F](executor)

  val uriRegex: Regex = """https://api.cloudflare.com/client/v4/zones/(.+?)/pagerules/(.+)""".r
}

class PageRuleClientImpl[F[_] : ApplicativeThrow](executor: StreamingCloudflareApiExecutor[F]) extends PageRuleClient[F] with Http4sClientDsl[F] {
  private def fetch(req: Request[F]): Stream[F, PageRule] =
    executor.fetch[PageRule](req)

  override def list(zoneId: ZoneId): Stream[F, PageRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "pagerules"))

  override def getById(zoneId: ZoneId, pageRuleId: String): Stream[F, PageRule] =
    fetch(GET(BaseUrl / "zones" / zoneId / "pagerules" / pageRuleId))

  override def create(zoneId: ZoneId, pageRule: PageRule): Stream[F, PageRule] =
    fetch(POST(pageRule.asJson, BaseUrl / "zones" / zoneId / "pagerules"))

  override def update(zoneId: ZoneId, pageRule: PageRule): Stream[F, PageRule] =
    // TODO it would really be better to do this check at compile time by baking the identification question into the types
    if (pageRule.id.isDefined)
      fetch(PUT(pageRule.copy(id = None).asJson, BaseUrl / "zones" / zoneId / "pagerules" / pageRule.id.get))
    else
      Stream.raiseError[F](CannotUpdateUnidentifiedPageRule(pageRule))

  override def delete(zoneId: ZoneId, pageRuleId: String): Stream[F, PageRuleId] =
    for {
      json <- executor.fetch[Json](DELETE(BaseUrl / "zones" / zoneId / "pagerules" / pageRuleId)).last.recover {
        case ex: UnexpectedCloudflareErrorException if ex.errors.flatMap(_.code.toSeq).exists(notFoundCodes.contains) =>
          None
      }
    } yield tagPageRuleId(json.flatMap(deletedRecordLens).getOrElse(pageRuleId))

  private val deletedRecordLens: Json => Option[String] = root.id.string.getOption
  private val notFoundCodes = List(1002, 7000, 7003)
}

case class CannotUpdateUnidentifiedPageRule(pageRule: PageRule) extends RuntimeException(s"Cannot update unidentified page rule $pageRule")
