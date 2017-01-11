package com.dwolla.lambda.cloudflare.record

import scala.language.{higherKinds, implicitConversions, postfixOps}

/**
  * Thanks to http://stackoverflow.com/a/8825823
  *
  * We could also use something from CATS or Scalaz, but would rather
  * avoid the overhead of including those libraries in the lambda jar
  *
  * @tparam Container the type of container e.g. <code>Option</code>
  */
trait Functor[Container[_]] {
  def map[A, B](x: Container[A], f: A => B): Container[B]
}

object Functor {
  implicit object optionFunctor extends Functor[Option] {
    override def map[A, B](x: Option[A], f: A ⇒ B): Option[B] = x.map(f)
  }

  implicit def liftConversion[F[_], A, B](x: F[A])(implicit f: A ⇒ B, functor: Functor[F]): F[B] = functor.map(x, f)
}
