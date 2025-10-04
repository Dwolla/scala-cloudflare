package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait RateLimitClientInstances {
  implicit val traceableValueAspect: Aspect[RateLimitClient, TraceableValue, TraceableValue] = Derive.aspect
}
