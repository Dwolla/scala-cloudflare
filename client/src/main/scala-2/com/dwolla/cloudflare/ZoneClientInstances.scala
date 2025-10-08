package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait ZoneClientInstances {
  implicit val traceableValueAspect: Aspect[ZoneClient, TraceableValue, TraceableValue] = Derive.aspect
}
