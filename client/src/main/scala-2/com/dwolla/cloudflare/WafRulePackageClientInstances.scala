package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait WafRulePackageClientInstances {
  implicit val traceableValueAspect: Aspect[WafRulePackageClient, TraceableValue, TraceableValue] = Derive.aspect
}
