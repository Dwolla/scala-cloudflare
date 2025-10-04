package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait FilterClientInstances {
  implicit val traceableValueAspect: Aspect[FilterClient, TraceableValue, TraceableValue] = Derive.aspect
}
