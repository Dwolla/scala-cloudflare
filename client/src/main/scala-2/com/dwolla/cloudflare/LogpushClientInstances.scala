package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait LogpushClientInstances {
  implicit val traceableValueAspect: Aspect[LogpushClient, TraceableValue, TraceableValue] = Derive.aspect
}
