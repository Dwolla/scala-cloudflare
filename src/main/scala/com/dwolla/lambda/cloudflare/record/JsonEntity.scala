package com.dwolla.lambda.cloudflare.record

import java.io.{OutputStream, OutputStreamWriter}

import org.apache.http.entity.EntityTemplate
import org.json4s.Formats
import org.json4s.native.Serialization._
import resource._

case class JsonEntity[T <: AnyRef](output: T)(implicit formats: Formats) extends EntityTemplate((out: OutputStream) ⇒ {
  for (writer ← managed(new OutputStreamWriter(out))) {
    write(output, writer)(formats)
  }
}) {
  setContentType("application/json")
}

object JsonEntity {
  import scala.language.implicitConversions

  implicit def anyRefToJsonEntity[T <: JsonWritable](x: T)(implicit formats: Formats): JsonEntity[T] = JsonEntity(x)
  implicit def anyRefToJsonEntity[T <: AnyRef, S <: JsonWritable](x: T)(implicit formats: Formats, ev: T ⇒ S): JsonEntity[S] = JsonEntity(ev(x))
}

trait JsonWritable extends AnyRef
