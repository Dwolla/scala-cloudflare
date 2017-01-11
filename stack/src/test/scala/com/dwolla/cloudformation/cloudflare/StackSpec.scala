package com.dwolla.cloudformation.cloudflare

import com.dwolla.lambda.cloudflare.record.CustomResourceHandler
import com.monsanto.arch.cloudformation.model._
import com.monsanto.arch.cloudformation.model.resource._
import org.specs2.matcher.ContainWithResult
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class StackSpec extends Specification {

  trait Setup extends Scope {
    val template = Stack.template()

    val s3Bucket = StringParameter("S3Bucket", "bucket where Lambda code can be found")
    val s3Key = StringParameter("S3Key", "key where Lambda code can be found")

    val role = `AWS::IAM::Role`("Role",
      AssumeRolePolicyDocument = PolicyDocument(Seq(
        PolicyStatement(
          Effect = "Allow",
          Principal = Option(DefinedPrincipal(Map("Service" → Seq("lambda.amazonaws.com")))),
          Action = Seq("sts:AssumeRole")
        )
      )),
      Policies = Option(Seq(
        Policy("Policy",
          PolicyDocument(Seq(
            PolicyStatement(
              Effect = "Allow",
              Action = Seq(
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
              ),
              Resource = Option("arn:aws:logs:*:*:*")
            ),
            PolicyStatement(
              Effect = "Allow",
              Action = Seq(
                "route53:GetHostedZone"
              ),
              Resource = Option("*")
            )
          ))
        )
      ))
    )

    val function = `AWS::Lambda::Function`("Function",
      Code = Code(
        S3Bucket = Option(s3Bucket),
        S3Key = Option(s3Key),
        S3ObjectVersion = None,
        ZipFile = None
      ),
      Description = Option("Creates or updates a public hostname at Cloudflare zone"),
      Handler = classOf[CustomResourceHandler].getName,
      Runtime = Java8,
      MemorySize = Some(512),
      Role = `Fn::GetAtt`(Seq(role.name, "Arn")),
      Timeout = Option(60)
    )
  }

  "Template" should {

    "define lambda function for correct handler class" in new Setup {
      template.lookupResource[`AWS::Lambda::Function`]("Function") must_== function
    }

    "define IAM role for the function" in new Setup {
      template.lookupResource[`AWS::IAM::Role`]("Role") must_== role
    }

    "define input parameters for S3 location so that info can come from sbt" in new Setup {
      template.Parameters must beSome(contain(s3Bucket.asInstanceOf[Parameter]))
      template.Parameters must beSome(contain(s3Key.asInstanceOf[Parameter]))
    }

    "have an appropriate description" in new Setup {
      template.Description must_== "cloudflare-public-hostname-lambda lambda function and supporting resources"
    }

    "export the lambda function" in new Setup {
      template.Outputs must beSome(thingThatContains(Output(
        "CloudflarePublicHostnameLambda",
        "ARN of the Lambda that interfaces with Cloudflare",
        `Fn::GetAtt`(Seq("Function", "Arn")),
        Some("CloudflarePublicHostnameLambda")
      )))
    }
  }

  def thingThatContains[R](output: Output[R]): ContainWithResult[Output[_]] = contain(output)
}
