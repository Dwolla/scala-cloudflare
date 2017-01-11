# Cloudflare DNS Record Handler

[![Travis](https://img.shields.io/travis/Dwolla/cloudflare-public-hostname-lambda.svg?style=flat-square)](https://travis-ci.org/Dwolla/cloudflare-public-hostname-lambda)
![license](https://img.shields.io/github/license/Dwolla/cloudflare-public-hostname-lambda.svg?style=flat-square)

An AWS CloudFormation custom resource that manages a Cloudflare DNS Record.

To run all tests:

```ShellSession
sbt clean 'test-only -- timefactor 10' 'stack/test-only -- timefactor 10' stack/it:test
```

## Deploy

To deploy the stack, ensure the required IAM roles exist (`DataEncrypter` and `cloudformation/deployer/cloudformation-deployer`), then deploy with `sbt`:

```ShellSession
sbt -DAWS_ACCOUNT_ID={your-account-id} publish stack/deploy
```

The `publish` task comes from [Dwolla’s S3 sbt plugin](https://github.com/Dwolla/sbt-s3-publisher), and the stack/deploy task comes from [Dwolla’s CloudFormation sbt plugin](https://github.com/Dwolla/sbt-cloudformation-stack).

## CloudFormation Custom Resource

Here is an example of how to include this as a custom resource in a CloudFormation stack.

```json
{
  "Parameters": {
    "CloudflareEmail": {
      "Description": "Email address of the account that can interact with the Cloudflare API",
      "Type": "String"
    },
    "CloudflareKey": {
      "Description": "Cloudflare API Key",
      "NoEcho": true,
      "Type": "String"
    }
  },
  "Resources": {
    "CloudflareRecord": {
      "Properties": {
        "Name": "example.dwolla.net",
        "Content": "example.us-west-2.sandbox.dwolla.net",
        "Type": "CNAME",
        "TTL": 42,
        "Proxied": true,

        "CloudflareEmail": {
          "Ref": "CloudflareEmail"
        },
        "CloudflareKey": {
          "Ref": "CloudflareKey"
        },
        "ServiceToken": {
          "Fn::ImportValue": "CloudflareDnsRecordLambda"
        }
      },
      "Type": "Custom::CloudflareDnsRecord"
    }
  }
}
```

There are five primary parameters defining the DNS record:

|Parameter Name|Type|Notes|
|--------------|----|-----|
|`Name`|String|The public-facing name of the DNS record. This is what can be resolved.|
|`Content`|String|This is the value of the record. For an `A` record, this should be an IP address. For a `CNAME`, it should be a hostname.|
|`Type`|one of: `A`, `CNAME`, or the other supported Cloudflare record types|May not be modified without deleting the existing record|
|`TTL`|Integer (seconds)|Optional TTL; if not set, Cloudflare assigns an automatic TTL|
|`Proxied`|boolean|Optional; indicates whether requests should be proxied through Cloudflare’s DDoS service.|
