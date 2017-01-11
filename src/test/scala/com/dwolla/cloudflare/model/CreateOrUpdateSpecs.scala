package com.dwolla.cloudflare.model

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future

class CreateOrUpdateSpecs(implicit ee: ExecutionEnv) extends Specification {

  trait Setup extends Scope

  "CreateOrUpdate" >> {

    "should have a value" in new Setup {
      val output: CreateOrUpdate[String] = Create("value")

      output.value must_== "value"
    }

    "Create" should {
      "return Some with the value when projected to create" in new Setup {
        val output = Create("value").create

        output must beSome("value")
      }

      "return None when projected to Update" in new Setup {
        val output = Create("value").update

        output must beNone
      }
    }

    "Update" should {

      "return None when projected to create" in new Setup {
        val output = Update("value").create

        output must beNone
      }

      "return Some with the value when projected to Update" in new Setup {
        val output = Update("value").update

        output must beSome("value")
      }
    }

    "CreateOrUpdate[Future[A]]" should {
      "be implicitly convertible to Future[CreateOrUpdate[A]]" >> {
        import CreateOrUpdate._

        val input = Create(Future.successful("value"))

        val output: Future[CreateOrUpdate[String]] = input

        output must beLike[CreateOrUpdate[String]] {
          case x ⇒ x.value must_== "value"
        }.await
      }
    }
  }
}
