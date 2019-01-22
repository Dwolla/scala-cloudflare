package com.dwolla.circe

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.{ DecodingFailure, Json }
import shapeless._
import shapeless.labelled.{ field, FieldType }

object EnumerationSnakeCodec extends EnumerationSnakeCodec

/** This is mainly copied from Circe's [[io.circe.generic.extras.encoding.EnumerationEncoder]] and
  * [[io.circe.generic.extras.decoding.EnumerationDecoder]], but since they're not configurable
  * to use snake_case for the named values, this version applies the
  * [[io.circe.generic.extras.Configuration.snakeCaseTransformation]] function to the names
  * during encoding and decoding.
  */
trait EnumerationSnakeCodec {
  abstract class EnumerationSnakeEncoder[A] extends Encoder[A]
  abstract class EnumerationSnakeDecoder[A] extends Decoder[A]

  implicit val encodeEnumerationCNil: EnumerationSnakeEncoder[CNil] = _ => sys.error("Cannot encode CNil")

  implicit def encodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](implicit
                                                                      wit: Witness.Aux[K],
                                                                      gv: LabelledGeneric.Aux[V, HNil],
                                                                      dr: EnumerationSnakeEncoder[R]
                                                                     ): EnumerationSnakeEncoder[FieldType[K, V] :+: R] = x => {
    val _ = gv
    x match {
      case Inl(_) => Json.fromString(Configuration.snakeCaseTransformation(wit.value.name))
      case Inr(r) => dr(r)
    }
  }

  implicit def encodeEnumeration[A, Repr <: Coproduct](implicit
                                                       gen: LabelledGeneric.Aux[A, Repr],
                                                       rr: EnumerationSnakeEncoder[Repr]
                                                      ): EnumerationSnakeEncoder[A] =
    a => rr(gen.to(a))

  def deriveEnumerationSnakeEncoder[A](implicit encode: Lazy[EnumerationSnakeEncoder[A]]): Encoder[A] = encode.value


  implicit val decodeEnumerationCNil: EnumerationSnakeDecoder[CNil] =
    c => Left(DecodingFailure("Enumeration", c.history))

  implicit def decodeEnumerationCCons[K <: Symbol, V, R <: Coproduct](implicit
                                                                      wit: Witness.Aux[K],
                                                                      gv: LabelledGeneric.Aux[V, HNil],
                                                                      dr: EnumerationSnakeDecoder[R]
                                                                     ): EnumerationSnakeDecoder[FieldType[K, V] :+: R] =
    c => c.as[String] match {
      case Right(s) if s == Configuration.snakeCaseTransformation(wit.value.name) => Right(Inl(field[K](gv.from(HNil))))
      case Right(_) => dr.apply(c).right.map(Inr(_))
      case Left(_) => Left(DecodingFailure("Enumeration", c.history))
    }

  implicit def decodeEnumeration[A, Repr <: Coproduct](implicit
                                                       gen: LabelledGeneric.Aux[A, Repr],
                                                       rr: EnumerationSnakeDecoder[Repr]
                                                      ): EnumerationSnakeDecoder[A] =
    rr(_).right.map(gen.from)

  def deriveEnumerationSnakeDecoder[A](implicit decode: Lazy[EnumerationSnakeDecoder[A]]): Decoder[A] = decode.value
}
