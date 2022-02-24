package com.dwolla.cloudflare

import com.dwolla.cloudflare.ZoneClientImpl.Domain
import com.dwolla.cloudflare.ZoneClientImpl.Status.active
import com.dwolla.cloudflare.domain.dto.dns._
import com.dwolla.cloudflare.domain.model._
import fs2._
import monix.newtypes.NewtypeWrapped
import org.http4s._

trait ZoneClient[F[_]] {
  def getZoneId(domain: String): Stream[F, ZoneId]
}

object ZoneClient {
  def apply[F[_]](executor: StreamingCloudflareApiExecutor[F]): ZoneClient[F] = new ZoneClientImpl[F](executor)
}

class ZoneClientImpl[F[_]](executor: StreamingCloudflareApiExecutor[F]) extends ZoneClient[F] {
  override def getZoneId(domain: String): Stream[F, ZoneId] =
    executor.fetch[ZoneDTO](Request[F](uri = BaseUrl / "zones" +*? Domain(domain) +*? active))
      .map(_.id)
      .collect {
        case Some(id) => tagZoneId(id)
      }
}

object ZoneClientImpl {
  type Domain = Domain.Type
  object Domain extends NewtypeWrapped[String] {
    implicit val queryParam: QueryParam[Domain] = new QueryParam[Domain] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[Domain] = value => QueryParameterValue(value.value)
  }

  sealed trait Status
  case object Active extends Status
  object Status {
    val active: Status = Active

    implicit val queryParam: QueryParam[Status] = new QueryParam[Status] {
      override def key: QueryParameterKey = QueryParameterKey("status")
    }
    implicit val queryParamEncoder: QueryParamEncoder[Status] = value => QueryParameterValue(value match {
      case Active => "active"
    })
  }
}
