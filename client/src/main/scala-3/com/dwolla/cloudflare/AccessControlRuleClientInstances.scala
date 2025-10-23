package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.accesscontrolrules.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccessControlRuleClientInstances:
  given Aspect[AccessControlRuleClient, TraceableValue, TraceableValue] =
    new Aspect[AccessControlRuleClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: AccessControlRuleClient[F]): AccessControlRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new AccessControlRuleClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def list(level: Level, mode: Option[String]): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRule] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("level", level),
                Aspect.Advice.byValue("mode", mode),
              )),
              Aspect.Advice("list", af.list(level, mode))
            )

          override def getById(level: Level, ruleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRule] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("level", level),
                Aspect.Advice.byValue("ruleId", ruleId),
              )),
              Aspect.Advice("getById", af.getById(level, ruleId))
            )

          override def create(level: Level, rule: AccessControlRule): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRule] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("level", level),
                Aspect.Advice.byValue("rule", rule),
              )),
              Aspect.Advice("create", af.create(level, rule))
            )

          override def update(level: Level, rule: AccessControlRule): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRule] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("level", level),
                Aspect.Advice.byValue("rule", rule),
              )),
              Aspect.Advice("update", af.update(level, rule))
            )

          override def delete(level: Level, ruleId: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRuleId] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("level", level),
                Aspect.Advice.byValue("ruleId", ruleId),
              )),
              Aspect.Advice("delete", af.delete(level, ruleId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, AccessControlRule] =
            Aspect.Weave(
              "AccessControlRuleClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: AccessControlRuleClient[F])(fk: F ~> G): AccessControlRuleClient[G] =
        new AccessControlRuleClient[G]:
          override def list(level: Level, mode: Option[String]): G[AccessControlRule] =
            fk(af.list(level, mode))

          override def getById(level: Level, ruleId: String): G[AccessControlRule] =
            fk(af.getById(level, ruleId))

          override def create(level: Level, rule: AccessControlRule): G[AccessControlRule] =
            fk(af.create(level, rule))

          override def update(level: Level, rule: AccessControlRule): G[AccessControlRule] =
            fk(af.update(level, rule))

          override def delete(level: Level, ruleId: String): G[AccessControlRuleId] =
            fk(af.delete(level, ruleId))

          override def getByUri(uri: String): G[AccessControlRule] =
            fk(af.getByUri(uri))
