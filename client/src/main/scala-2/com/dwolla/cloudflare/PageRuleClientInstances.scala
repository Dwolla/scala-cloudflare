package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait PageRuleClientInstances {
  implicit val traceableValueAspect: Aspect[PageRuleClient, TraceableValue, TraceableValue] = Derive.aspect
}
