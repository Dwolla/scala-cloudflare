package com.dwolla.cloudformation.cloudflare

import com.dwolla.lambda.cloudflare.record.CustomResourceHandler
import com.monsanto.arch.cloudformation.model._
import com.monsanto.arch.cloudformation.model.resource._

object Stack {
  def template(): Template = {
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

    val s3Bucket = StringParameter("S3Bucket", "bucket where Lambda code can be found")
    val s3Key = StringParameter("S3Key", "key where Lambda code can be found")

    val key = `AWS::KMS::Key`("Key",
      Option("Encryption key protecting secrets for the Cloudflare public record lambda"),
      Enabled = Option(true),
      EnableKeyRotation = Option(true),
      KeyPolicy = PolicyDocument(
        Seq(
          PolicyStatement(
            Sid = Option("AllowDataEncrypterToEncrypt"),
            Effect = "Allow",
            Principal = Option(DefinedPrincipal(Map("AWS" → Seq(`Fn::Sub`("arn:aws:iam::${AWS::AccountId}:role/DataEncrypter"))))),
            Action = Seq(
              "kms:Encrypt",
              "kms:ReEncrypt",
              "kms:DescribeKey"
            ),
            Resource = Option("*")
          ),
          PolicyStatement(
            Sid = Option("AllowLambdaToDecrypt"),
            Effect = "Allow",
            Principal = Option(DefinedPrincipal(Map("AWS" → Seq(`Fn::GetAtt`(Seq(role.name, "Arn")))))),
            Action = Seq(
              "kms:Decrypt",
              "kms:DescribeKey"
            ),
            Resource = Option("*")
          ),
          PolicyStatement(
            Sid = Option("CloudFormationDeploymentRoleOwnsKey"),
            Effect = "Allow",
            Principal = Option(DefinedPrincipal(Map("AWS" → Seq(`Fn::Sub`("arn:aws:iam::${AWS::AccountId}:role/cloudformation/deployer/cloudformation-deployer"))))),
            Action = Seq(
              "kms:Create*",
              "kms:Describe*",
              "kms:Enable*",
              "kms:List*",
              "kms:Put*",
              "kms:Update*",
              "kms:Revoke*",
              "kms:Disable*",
              "kms:Get*",
              "kms:Delete*",
              "kms:ScheduleKeyDeletion",
              "kms:CancelKeyDeletion"
            ),
            Resource = Option("*")
          )
        )
      )
    )

    val alias = `AWS::KMS::Alias`("KeyAlias", AliasName = "alias/CloudflarePublicDnsRecordKey", TargetKeyId = ResourceRef(key))

    val lambda = `AWS::Lambda::Function`("Function",
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

    Template(
      Description = "cloudflare-public-hostname-lambda lambda function and supporting resources",
      Parameters = Option(Seq(
        s3Bucket,
        s3Key
      )),
      Resources = Option(Seq(role, lambda, key, alias)),
      Conditions = None,
      Mappings = None,
      Routables = None,
      Outputs = Some(Seq(
        Output(
          "CloudflarePublicHostnameLambda",
          "ARN of the Lambda that interfaces with Cloudflare",
          `Fn::GetAtt`(Seq(lambda.name, "Arn")),
          Some("CloudflarePublicHostnameLambda")
        ),
        Output(
          "CloudflarePublicHostnameKey",
          "KMS Key Alias for Cloudflare public DNS record lambda",
          ResourceRef(alias)
        )
      ))
    )
  }
}
