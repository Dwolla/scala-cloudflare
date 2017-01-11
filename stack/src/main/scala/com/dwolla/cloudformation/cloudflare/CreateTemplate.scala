package com.dwolla.cloudformation.cloudflare

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import spray.json._

import scala.language.implicitConversions

object CreateTemplate extends App {
  val template = Stack.template()

  private val outputFilename = args(0)
  Files.write(Paths.get(outputFilename), template.toJson.prettyPrint.getBytes(StandardCharsets.UTF_8))
}
