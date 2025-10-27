package com.dwolla.cloudflare

import monix.newtypes.integrations.DerivedCirceCodec
import monix.newtypes.{HasExtractor, NewtypeWrapped}
import natchez.TraceableValue

abstract class CloudflareNewtype[A: TraceableValue] extends NewtypeWrapped[A] with DerivedCirceCodec {
  final def unapply(a: A): Some[Type] =
    Some(apply(a))

  implicit val traceableValue: TraceableValue[Type] = TraceableValue[A].contramap(implicitly[HasExtractor.Aux[Type, A]].extract)
}
