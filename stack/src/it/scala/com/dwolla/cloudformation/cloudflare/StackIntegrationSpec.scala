package com.dwolla.cloudformation.cloudflare

import com.amazonaws.regions.Regions._
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClientBuilder
import com.amazonaws.services.cloudformation.model.{ValidateTemplateRequest, ValidateTemplateResult}
import com.dwolla.awssdk.utils.ScalaAsyncHandler.Implicits._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.{After, Specification}
import spray.json._

import scala.concurrent.duration._

class StackIntegrationSpec(implicit val ee: ExecutionEnv) extends Specification {

  trait Setup extends After {
    val client = AmazonCloudFormationAsyncClientBuilder.standard().withRegion(US_WEST_2).build()

    override def after = client.shutdown()
  }

  "Stack Template" should {
    "validate using Amazon's online validation service" in new Setup {

      val request = new ValidateTemplateRequest().withTemplateBody(Stack.template().toJson.prettyPrint)

      val output = request.via(client.validateTemplateAsync)

      output.map(_.getDescription) must be_==("cloudflare-public-hostname-lambda lambda function and supporting resources").await(0, 10.seconds)
    }
  }
}
