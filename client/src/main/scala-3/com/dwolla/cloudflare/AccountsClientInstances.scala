package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.accounts.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccountsClientInstances:
  given Aspect[AccountsClient, TraceableValue, TraceableValue] =
    new Aspect[AccountsClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: AccountsClient[F]): AccountsClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new AccountsClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(): Aspect.Weave[F, TraceableValue, TraceableValue, Account] =
            Aspect.Weave(
              "AccountsClient",
              List(Nil),
              Aspect.Advice("list", af.list())
            )

          override def getById(accountId: String): Aspect.Weave[F, TraceableValue, TraceableValue, Account] =
            Aspect.Weave(
              "AccountsClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
              )),
              Aspect.Advice("getById", af.getById(accountId))
            )

          override def getByName(name: String): Aspect.Weave[F, TraceableValue, TraceableValue, Account] =
            Aspect.Weave(
              "AccountsClient",
              List(List(
                Aspect.Advice.byValue("name", name),
              )),
              Aspect.Advice("getByName", af.getByName(name))
            )

          override def listRoles(accountId: AccountId): Aspect.Weave[F, TraceableValue, TraceableValue, AccountRole] =
            Aspect.Weave(
              "AccountsClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
              )),
              Aspect.Advice("listRoles", af.listRoles(accountId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, Account] =
            Aspect.Weave(
              "AccountsClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: AccountsClient[F])(fk: F ~> G): AccountsClient[G] =
        new AccountsClient[G]:
          override def list(): G[Account] = fk(af.list())
          override def getById(accountId: String): G[Account] = fk(af.getById(accountId))
          override def getByName(name: String): G[Account] = fk(af.getByName(name))
          override def listRoles(accountId: AccountId): G[AccountRole] = fk(af.listRoles(accountId))
          override def getByUri(uri: String): G[Account] = fk(af.getByUri(uri))
