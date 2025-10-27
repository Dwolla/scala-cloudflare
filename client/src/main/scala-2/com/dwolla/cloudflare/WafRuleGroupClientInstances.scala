package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRuleGroupClientInstances {
  implicit val traceableValueAspect: Aspect[WafRuleGroupClient, TraceableValue, TraceableValue] = Derive.aspect
}
