package com.dwolla.lambda.cloudflare.record

import com.amazonaws.services.kms.model.AWSKMSException
import com.dwolla.awssdk.kms.KmsDecrypter
import com.dwolla.cloudflare.model.{IdentifiedDnsRecord, UnidentifiedDnsRecord}
import com.dwolla.cloudflare.{CloudflareApiExecutor, DnsRecordClient, DnsRecordIdDoesNotExistException}
import com.dwolla.lambda.cloudformation.{CloudFormationCustomResourceRequest, HandlerResponse}
import com.dwolla.testutils.exceptions.NoStackTraceException
import com.dwolla.testutils.mocking.WithBehaviorMocking
import org.json4s.JValue
import org.json4s.JsonAST.JString
import org.mockito.Matchers.startsWith
import org.slf4j.Logger
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CloudflareDnsRecordHandlerSpec extends Specification with Mockito with WithBehaviorMocking {
  implicit val ee: ExecutionEnv = ExecutionEnv.fromGlobalExecutionContext

  trait Setup extends Scope {
    val mockCloudflareApiClient = mock[DnsRecordClient]
    val mockCloudflareApiExecutor = mock[CloudflareApiExecutor]
    val mockLogger = mock[Logger]
    val mockKmsDecrypter = mock[KmsDecrypter]

    mockKmsDecrypter.decryptBase64("CloudflareEmail" → "cloudflare-account-email@dwollalabs.com", "CloudflareKey" → "fake-key") returns Future.successful(Map("CloudflareEmail" → "cloudflare-account-email@dwollalabs.com", "CloudflareKey" → "fake-key").transform((_, value) ⇒ value.getBytes("UTF-8")))

    val handler = new CloudflareDnsRecordHandler() {
      override protected def cloudFlareApiExecutor(email: String, key: String) = {
        if (!promisedCloudflareApiExecutor.isCompleted)
          promisedCloudflareApiExecutor.success(mockCloudflareApiExecutor)

        promisedCloudflareApiExecutor.future
      }

      override protected lazy val logger = mockLogger

      override protected def cloudFlareApiClient(executor: CloudflareApiExecutor) = mockCloudflareApiClient

      override protected lazy val kmsDecrypter: KmsDecrypter = mockKmsDecrypter
    }
  }

  "CloudflareDnsRecordHandler create" should {

    "create specified dns record" in new Setup {
      private val expectedRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(42),
        proxied = Option(true)
      )

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(None)
      mockCloudflareApiClient.createDnsRecord(
        UnidentifiedDnsRecord(
          name = "example.dwolla.com",
          content = "example.dwollalabs.com",
          recordType = "CNAME",
          ttl = Option(42),
          proxied = Option(true)
        )) returns Future.successful(expectedRecord)

      val request = buildRequest(
        requestType = "CrEaTe",
        physicalResourceId = None,
        resourceProperties = Some(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id"
          handlerResponse.data must havePair("dnsRecord" → expectedRecord)
          handlerResponse.data must havePair("created" → Some(expectedRecord))
          handlerResponse.data must havePair("updated" → None)
          handlerResponse.data must havePair("oldDnsRecord" → None)
      }.await
    }

    "log failure and close the clients if creation fails" in new Setup {
      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(None)
      mockCloudflareApiClient.createDnsRecord(
        UnidentifiedDnsRecord(
          name = "example.dwolla.com",
          content = "example.dwollalabs.com",
          recordType = "CNAME",
          ttl = Option(42),
          proxied = Option(true)
        )) returns Future.failed(NoStackTraceException)

      val request = buildRequest(
        requestType = "CrEaTe",
        physicalResourceId = None,
        resourceProperties = Some(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA(NoStackTraceException).await
    }

    "propagate exception if fetching existing records fails" in new Setup {
      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.failed(NoStackTraceException)

      val request = buildRequest(
        requestType = "CrEaTe",
        physicalResourceId = None,
        resourceProperties = Some(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA(NoStackTraceException).await
    }

    "create a DNS record if it doesn't exist, despite having a physical ID provided by CloudFormation" in new Setup {
      private val providedPhysicalId = Option("https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id")
      private val expectedRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(42),
        proxied = Option(true)
      )

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(None)
      mockCloudflareApiClient.createDnsRecord(expectedRecord.copy(content = "new-example.dwollalabs.com").unidentify) returns Future.successful(expectedRecord)

      val request = buildRequest(
        requestType = "update",
        physicalResourceId = providedPhysicalId,
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== expectedRecord.physicalResourceId
          handlerResponse.data must havePair("dnsRecord" → expectedRecord)
          handlerResponse.data must havePair("oldDnsRecord" → None)
      }.await
    }
  }

  "CloudflareDnsRecordHandler update" should {
    "update a DNS record if it already exists, even if no physical ID is passed in by CloudFormation" in new Setup {
      private val existingRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(42),
        proxied = Option(true)
      )

      private val expectedRecord = existingRecord.copy(content = "new-example.dwollalabs.com")

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(Option(existingRecord))
      mockCloudflareApiClient.updateDnsRecord(expectedRecord) returns Future.successful(expectedRecord)

      val request = buildRequest(
        requestType = "CrEaTe",
        physicalResourceId = None,
        resourceProperties = Some(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== expectedRecord.physicalResourceId
          handlerResponse.data must havePair("dnsRecord" → expectedRecord)
          handlerResponse.data must havePair("oldDnsRecord" → Option(existingRecord))
      }.await

      there was one(mockLogger).warn(startsWith("""Discovered DNS record ID "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id" for hostname "example.dwolla.com""""))
    }

    "update a DNS record if it already exists, even if the physical ID passed in by CloudFormation doesn't match the existing ID (returning the new ID)" in new Setup {
      private val existingRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(42),
        proxied = Option(true)
      )
      private val expectedRecord = existingRecord.copy(content = "new-example.dwollalabs.com")

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(Option(existingRecord))
      mockCloudflareApiClient.updateDnsRecord(expectedRecord) returns Future.successful(expectedRecord)

      val request = buildRequest(
        requestType = "update",
        physicalResourceId = Option("different-physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== expectedRecord.physicalResourceId
          handlerResponse.data must havePair("dnsRecord" → expectedRecord)
          handlerResponse.data must havePair("oldDnsRecord" → Option(existingRecord))
      }.await

      there was one(mockLogger).warn(startsWith(
        """The passed physical ID "different-physical-id" does not match the discovered physical ID "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id" for hostname "example.dwolla.com"."""))
    }

    "refuse to change the record type" in new Setup {
      private val existingRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "A",
        ttl = Option(42),
        proxied = Option(true)
      )

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(Option(existingRecord))

      val request = buildRequest(
        requestType = "update",
        physicalResourceId = Option("different-physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA(DnsRecordTypeChange("A", "CNAME")).await
    }

    "propagate the failure exception if update fails" in new Setup {
      private val existingRecord = IdentifiedDnsRecord(
        physicalResourceId = "https://api.cloudflare.com/client/v4/zones/fake-zone-id/dns_records/fake-resource-id",
        zoneId = "fake-zone-id",
        resourceId = "fake-resource-id",
        name = "example.dwolla.com",
        content = "example.dwollalabs.com",
        recordType = "CNAME",
        ttl = Option(42),
        proxied = Option(true)
      )

      mockCloudflareApiClient.getExistingDnsRecord("example.dwolla.com") returns Future.successful(Option(existingRecord))
      mockCloudflareApiClient.updateDnsRecord(any[IdentifiedDnsRecord]) returns Future.failed(NoStackTraceException)

      val request = buildRequest(
        requestType = "update",
        physicalResourceId = Option("different-physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA(NoStackTraceException).await
    }

    "propagate exceptions thrown by the KMS decrypter" >> {
      val kmsExceptionMessage = "The ciphertext refers to a customer master key that does not exist, does not exist in this region, or you are not allowed to access"

      val mockKms = mock[KmsDecrypter] withBehavior {_.decryptBase64(any)(any[ExecutionContext]) returns Future.failed(new AWSKMSException(kmsExceptionMessage))}
      val handler = new CloudflareDnsRecordHandler {
        override protected lazy val kmsDecrypter: KmsDecrypter = mockKms
      }

      val request = buildRequest(
        requestType = "update",
        physicalResourceId = Option("different-physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString("new-example.dwollalabs.com"),
          "Type" → JString("CNAME"),
          "TTL" → JString("42"),
          "Proxied" → JString("true"),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA[AWSKMSException].like { case ex ⇒ ex.getMessage must startWith(kmsExceptionMessage) }.await
    }
  }

  "CloudflareDnsRecordHandler delete" should {
    "delete a DNS record if requested" in new Setup {
      val request = buildRequest(
        requestType = "delete",
        physicalResourceId = Option("physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString(""),
          "Type" → JString(""),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      mockCloudflareApiClient.deleteDnsRecord("physical-id") returns Future.successful("physical-id")

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== "physical-id"
          handlerResponse.data must havePair("deletedRecordId" → "physical-id")
      }.await
    }

    "delete is successful even if the physical ID passed by CloudFormation doesn't exist" in new Setup {
      mockCloudflareApiClient.deleteDnsRecord("physical-id") returns Future.failed(DnsRecordIdDoesNotExistException("fake-url"))

      val request = buildRequest(
        requestType = "delete",
        physicalResourceId = Option("physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString(""),
          "Type" → JString(""),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must beLike[HandlerResponse] {
        case handlerResponse ⇒
          handlerResponse.physicalId must_== "physical-id"
          handlerResponse.data must not(havePair("deletedRecordId" → "physical-id"))
      }.await

      there was one(mockLogger).error("The record could not be deleted because it did not exist; nonetheless, responding with Success!",
        DnsRecordIdDoesNotExistException("fake-url"))
    }

    "log failure and close the clients if delete fails" in new Setup {
      mockCloudflareApiClient.deleteDnsRecord("physical-id") returns Future.failed(NoStackTraceException)

      val request = buildRequest(
        requestType = "delete",
        physicalResourceId = Option("physical-id"),
        resourceProperties = Option(Map(
          "Name" → JString("example.dwolla.com"),
          "Content" → JString(""),
          "Type" → JString(""),
          "CloudflareEmail" → JString("cloudflare-account-email@dwollalabs.com"),
          "CloudflareKey" → JString("fake-key")
        ))
      )

      val output = handler.handleRequest(request)

      output must throwA(NoStackTraceException).await
    }
  }

  "CloudflareDnsRecordHandler shutdown" should {
    "close the clients" in new Setup {
      handler.promisedCloudflareApiExecutor.success(mockCloudflareApiExecutor)

      handler.shutdown()

      Await.ready(handler.promisedCloudflareApiExecutor.future, 30.seconds)

      there was one(mockCloudflareApiExecutor).close()
    }

  }
  "Exceptions" >> {
    "DnsRecordTypeChange" should {
      "mention the existing and new record types" >> {
        DnsRecordTypeChange("existing", "new") must beLikeA[RuntimeException] {
          case ex ⇒ ex.getMessage must_== """Refusing to change DNS record from "existing" to "new"."""
        }
      }
    }
  }

  private def buildRequest(requestType: String,
                           physicalResourceId: Option[String],
                           resourceProperties: Option[Map[String, JValue]]) =
    CloudFormationCustomResourceRequest(
      RequestType = requestType,
      ResponseURL = "",
      StackId = "",
      RequestId = "",
      ResourceType = "",
      LogicalResourceId = "",
      PhysicalResourceId = physicalResourceId,
      ResourceProperties = resourceProperties,
      OldResourceProperties = None
    )

}

case class CustomNoStackTraceException(msg: String, ex: Throwable = null) extends RuntimeException(msg, ex, true, false)
