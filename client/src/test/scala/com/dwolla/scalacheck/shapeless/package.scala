package com.dwolla.scalacheck

import org.scalacheck.*
import monix.newtypes.*

package object shapeless {
  implicit def arbitraryNewtype[A, B](implicit HB: HasBuilder.Aux[A, B], AB: Arbitrary[B]): Arbitrary[A] =
    Arbitrary(Arbitrary.arbitrary[B].flatMap(HB.build(_).fold(_ => Gen.fail, Gen.const)))
}
