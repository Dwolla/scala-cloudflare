package com.dwolla.lambda.cloudflare.record

import org.specs2.mutable.Specification

class CustomResourceHandlerSpec extends Specification {

  "CustomResourceHandler" should {
    "create a new instance every time createParsedRequestHandler is called" >> {
      val classToTest = new CustomResourceHandler

      val instances = Seq.range(0, 2).map(_ ⇒ classToTest.createParsedRequestHandler()).toSet

      instances must haveLength(2)
    }
  }

}
