package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccountsClientInstances {
  implicit val traceableValueAspect: Aspect[AccountsClient, TraceableValue, TraceableValue] = Derive.aspect
}
