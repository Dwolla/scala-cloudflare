package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.firewallrules.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait FirewallRuleClientInstances:
  given Aspect[FirewallRuleClient, TraceableValue, TraceableValue] =
    new Aspect[FirewallRuleClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: FirewallRuleClient[F]): FirewallRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new FirewallRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(zoneId: ZoneId): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRule] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
              )),
              Aspect.Advice("list", af.list(zoneId))
            )

          override def getById(zoneId: ZoneId, firewallRuleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRule] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("firewallRuleId", firewallRuleId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, firewallRuleId))
            )

          override def create(zoneId: ZoneId, firewallRule: FirewallRule): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRule] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("firewallRule", firewallRule),
              )),
              Aspect.Advice("create", af.create(zoneId, firewallRule))
            )

          override def update(zoneId: ZoneId, firewallRule: FirewallRule): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRule] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("firewallRule", firewallRule),
              )),
              Aspect.Advice("update", af.update(zoneId, firewallRule))
            )

          override def delete(zoneId: ZoneId, firewallRuleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRuleId] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("firewallRuleId", firewallRuleId),
              )),
              Aspect.Advice("delete", af.delete(zoneId, firewallRuleId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, FirewallRule] =
            Aspect.Weave(
              "FirewallRuleClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: FirewallRuleClient[F])(fk: F ~> G): FirewallRuleClient[G] =
        new FirewallRuleClient[G]:
          override def list(zoneId: ZoneId): G[FirewallRule] = fk(af.list(zoneId))
          override def getById(zoneId: ZoneId, firewallRuleId: String): G[FirewallRule] = fk(af.getById(zoneId, firewallRuleId))
          override def create(zoneId: ZoneId, firewallRule: FirewallRule): G[FirewallRule] = fk(af.create(zoneId, firewallRule))
          override def update(zoneId: ZoneId, firewallRule: FirewallRule): G[FirewallRule] = fk(af.update(zoneId, firewallRule))
          override def delete(zoneId: ZoneId, firewallRuleId: String): G[FirewallRuleId] = fk(af.delete(zoneId, firewallRuleId))
          override def getByUri(uri: String): G[FirewallRule] = fk(af.getByUri(uri))
