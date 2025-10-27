package com.dwolla.cloudflare

import cats.tagless.aop.*
import cats.tagless.Derive
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait DnsRecordClientInstances {
  implicit val traceableValueAspect: Aspect[DnsRecordClient, TraceableValue, TraceableValue] = Derive.aspect
}
