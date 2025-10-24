package com.dwolla.cloudflare

import monix.newtypes.NewtypeWrapped
import monix.newtypes.integrations.DerivedCirceCodec

abstract class CloudflareNewtype[A] extends NewtypeWrapped[A] with DerivedCirceCodec {
  final def unapply(a: A): Some[Type] =
    Some(apply(a))
}
