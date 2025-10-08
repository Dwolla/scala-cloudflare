package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRuleClientInstances {
  implicit val traceableValueAspect: Aspect[WafRuleClient, TraceableValue, TraceableValue] = Derive.aspect
}
