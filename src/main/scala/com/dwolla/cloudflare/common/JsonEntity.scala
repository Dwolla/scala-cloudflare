package com.dwolla.cloudflare.common

import java.io.{OutputStream, OutputStreamWriter}

import org.apache.http.entity.EntityTemplate
import org.json4s.Formats
import org.json4s.native.Serialization._
import resource._

private[cloudflare] case class JsonEntity[T <: AnyRef](output: T)(implicit formats: Formats) extends EntityTemplate((out: OutputStream) ⇒ {
  for (writer ← managed(new OutputStreamWriter(out))) {
    write(output, writer)(formats)
  }
}) {
  setContentType("application/json")
}

private[cloudflare] object JsonEntity {
  import scala.language.implicitConversions

  implicit def anyRefToJsonEntity[T <: JsonWritable](x: T)(implicit formats: Formats): JsonEntity[T] = JsonEntity(x)
  implicit def anyRefToJsonEntity[T <: AnyRef, S <: JsonWritable](x: T)(implicit formats: Formats, ev: T ⇒ S): JsonEntity[S] = JsonEntity(ev(x))
}

private[cloudflare] trait JsonWritable extends AnyRef
