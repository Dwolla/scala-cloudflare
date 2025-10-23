package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait AccountMembersClientInstances {
  implicit val traceableValueAspect: Aspect[AccountMembersClient, TraceableValue, TraceableValue] = Derive.aspect
}
