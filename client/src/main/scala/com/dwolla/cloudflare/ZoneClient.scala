package com.dwolla.cloudflare

import cats.tagless.aop.Aspect
import cats.tagless.syntax.all.*
import cats.*
import cats.effect.{Trace as _, *}
import com.dwolla.cloudflare.domain.dto.dns.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.tracing.TraceWeaveCapturingInputsAndOutputs
import fs2.*
import natchez.*
import org.http4s.*

trait ZoneClient[F[_]] {
  def getZoneId(domain: String): F[ZoneId]
}

object ZoneClient extends ZoneClientInstances {
  def apply[F[_] : MonadCancelThrow : Trace](executor: StreamingCloudflareApiExecutor[F]): ZoneClient[Stream[F, *]] =
    apply(executor, new TraceWeaveCapturingInputsAndOutputs)

  def apply[F[_], Dom[_], Cod[_]](executor: StreamingCloudflareApiExecutor[F],
                                  transform: Aspect.Weave[Stream[F, *], Dom, Cod, *] ~> Stream[F, *])
                                 (implicit A: Aspect[ZoneClient, Dom, Cod]): ZoneClient[Stream[F, *]] =
    A.mapK(ZoneClientImpl[F](executor).weave[Dom, Cod])(transform)

  private class ZoneClientImpl[F[_]] private[ZoneClient] (executor: StreamingCloudflareApiExecutor[F]) extends ZoneClient[Stream[F, *]] {
    override def getZoneId(domain: String): Stream[F, ZoneId] =
      executor.fetch[ZoneDTO](Request[F](uri = BaseUrl / "zones" +*? Domain(domain) +*? Status.active))
        .map(_.id)
        .collect {
          case Some(id) => tagZoneId(id)
        }
  }

  object ZoneClientImpl {
    def apply[F[_]](executor: StreamingCloudflareApiExecutor[F]): ZoneClient[Stream[F, *]] =
      new ZoneClientImpl[F](executor)
  }

  private type Domain = Domain.Type
  private object Domain extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[Domain] = new QueryParam[Domain] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[Domain] = value => QueryParameterValue(value.value)
  }

  private sealed trait Status
  private case object Active extends Status
  private object Status {
    val active: Status = Active

    implicit val queryParam: QueryParam[Status] = new QueryParam[Status] {
      override def key: QueryParameterKey = QueryParameterKey("status")
    }
    implicit val queryParamEncoder: QueryParamEncoder[Status] = value => QueryParameterValue(value match {
      case Active => "active"
    })
  }
}
