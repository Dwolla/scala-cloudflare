package com.dwolla.cloudflare

import _root_.io.circe.generic.auto._
import cats.effect._
import com.dwolla.cloudflare.domain.dto.dns._
import com.dwolla.cloudflare.domain.model._
import fs2._
import org.http4s._

import scala.language.higherKinds

trait ZoneClient[F[_]] {
  def getZoneId(domain: String): Stream[F, ZoneId]
}

object ZoneClient {
  def apply[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]): ZoneClient[F] = new ZoneClientImpl[F](executor)
}

class ZoneClientImpl[F[_] : Sync](executor: StreamingCloudflareApiExecutor[F]) extends ZoneClient[F] {
  override def getZoneId(domain: String): Stream[F, ZoneId] =
    executor.fetch[ZoneDTO](Request[F](uri = BaseUrl / "zones" +? ("name", domain) +? ("status", "active")))
      .map(_.id)
      .collect {
        case Some(id) ⇒ tagZoneId(id)
      }
}
