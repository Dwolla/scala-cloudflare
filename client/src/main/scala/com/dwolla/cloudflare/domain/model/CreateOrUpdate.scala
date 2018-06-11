package com.dwolla.cloudflare.domain.model

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

sealed trait CreateOrUpdate[+A] extends Product with Serializable {
  val value: A

  def create: Option[A]

  def update: Option[A]
}

final case class Create[A](value: A) extends CreateOrUpdate[A] {
  override def create: Option[A] = Some(value)

  override def update: Option[A] = None
}

final case class Update[A](value: A) extends CreateOrUpdate[A] {
  override def create: Option[A] = None

  override def update: Option[A] = Some(value)
}

object CreateOrUpdate {
  implicit def invertFuture[A](x: CreateOrUpdate[Future[A]])(implicit ec: ExecutionContext): Future[CreateOrUpdate[A]] = x match {
    case Create(future) ⇒ future.map(Create(_))
    case Update(future) ⇒ future.map(Update(_))
  }
}
