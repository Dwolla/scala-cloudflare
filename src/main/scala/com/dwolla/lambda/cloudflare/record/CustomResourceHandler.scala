package com.dwolla.lambda.cloudflare.record

import com.dwolla.lambda.cloudformation.AbstractCustomResourceHandler

import scala.concurrent.ExecutionContext

class CustomResourceHandler extends AbstractCustomResourceHandler {
  override def createParsedRequestHandler() = new CloudflareDnsRecordHandler

  override implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}
