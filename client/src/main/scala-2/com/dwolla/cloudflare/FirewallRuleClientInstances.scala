package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait FirewallRuleClientInstances {
  implicit val traceableValueAspect: Aspect[FirewallRuleClient, TraceableValue, TraceableValue] = Derive.aspect
}
