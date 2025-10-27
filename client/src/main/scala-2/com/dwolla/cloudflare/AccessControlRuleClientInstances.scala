package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccessControlRuleClientInstances {
  implicit val traceableValueAspect: Aspect[AccessControlRuleClient, TraceableValue, TraceableValue] = Derive.aspect
}
