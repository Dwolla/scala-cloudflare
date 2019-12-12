package com.dwolla

import cats.implicits._
import io.circe.{Decoder, Encoder}
import shapeless.tag.@@

package object circe {
  implicit def encodeTaggedString[A]: Encoder[String @@ A] = Encoder[String].narrow
  implicit def decodeTaggedString[A]: Decoder[String @@ A] = Decoder[String].map(shapeless.tag[A][String])
  implicit def encodeTaggedInt[A]: Encoder[Int @@ A] = Encoder[Int].narrow
  implicit def decodeTaggedInt[A]: Decoder[Int @@ A] = Decoder[Int].map(shapeless.tag[A][Int])
}
