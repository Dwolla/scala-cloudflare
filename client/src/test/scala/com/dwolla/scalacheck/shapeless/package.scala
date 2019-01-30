package com.dwolla.scalacheck

import org.scalacheck.Arbitrary
import _root_.shapeless.tag.@@
import _root_.shapeless.tag

package object shapeless {
  implicit def arbitraryTaggedString[A]: Arbitrary[String @@ A] = Arbitrary(Arbitrary.arbitrary[String].map(tag[A][String](_)))
}
