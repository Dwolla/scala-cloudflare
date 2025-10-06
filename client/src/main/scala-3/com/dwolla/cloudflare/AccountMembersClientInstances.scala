package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.accounts.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccountMembersClientInstances:
  given Aspect[AccountMembersClient, TraceableValue, TraceableValue] =
    new Aspect[AccountMembersClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: AccountMembersClient[F]): AccountMembersClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new AccountMembersClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def getById(accountId: AccountId, memberId: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccountMember] =
            Aspect.Weave(
              "AccountMembersClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
                Aspect.Advice.byValue("memberId", memberId),
              )),
              Aspect.Advice("getById", af.getById(accountId, memberId))
            )

          override def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): Aspect.Weave[F, TraceableValue, TraceableValue, AccountMember] =
            Aspect.Weave(
              "AccountMembersClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
                Aspect.Advice.byValue("emailAddress", emailAddress),
                Aspect.Advice.byValue("roleIds", roleIds),
              )),
              Aspect.Advice("addMember", af.addMember(accountId, emailAddress, roleIds))
            )

          override def updateMember(accountId: AccountId, accountMember: AccountMember): Aspect.Weave[F, TraceableValue, TraceableValue, AccountMember] =
            Aspect.Weave(
              "AccountMembersClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
                Aspect.Advice.byValue("accountMember", accountMember),
              )),
              Aspect.Advice("updateMember", af.updateMember(accountId, accountMember))
            )

          override def removeMember(accountId: AccountId, accountMemberId: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccountMemberId] =
            Aspect.Weave(
              "AccountMembersClient",
              List(List(
                Aspect.Advice.byValue("accountId", accountId),
                Aspect.Advice.byValue("accountMemberId", accountMemberId),
              )),
              Aspect.Advice("removeMember", af.removeMember(accountId, accountMemberId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccountMember] =
            Aspect.Weave(
              "AccountMembersClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: AccountMembersClient[F])(fk: F ~> G): AccountMembersClient[G] =
        new AccountMembersClient[G]:
          override def getById(accountId: AccountId, memberId: String): G[AccountMember] =
            fk(af.getById(accountId, memberId))

          override def addMember(accountId: AccountId, emailAddress: String, roleIds: List[String]): G[AccountMember] =
            fk(af.addMember(accountId, emailAddress, roleIds))

          override def updateMember(accountId: AccountId, accountMember: AccountMember): G[AccountMember] =
            fk(af.updateMember(accountId, accountMember))

          override def removeMember(accountId: AccountId, accountMemberId: String): G[AccountMemberId] =
            fk(af.removeMember(accountId, accountMemberId))

          override def getByUri(uri: String): G[AccountMember] =
            fk(af.getByUri(uri))
