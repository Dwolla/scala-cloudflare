package com.dwolla.lambda.cloudflare.record

import com.dwolla.awssdk.kms.KmsDecrypter
import com.dwolla.cloudflare.model._
import com.dwolla.cloudflare.{CloudflareApiExecutor, CloudflareAuthorization, DnsRecordClient, DnsRecordIdDoesNotExistException}
import com.dwolla.lambda.cloudformation.{CloudFormationCustomResourceRequest, HandlerResponse, MissingResourceProperties, ParsedCloudFormationCustomResourceRequestHandler}
import org.json4s.{DefaultFormats, Formats, JValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.{higherKinds, implicitConversions, postfixOps, reflectiveCalls}

class CloudflareDnsRecordHandler(implicit ec: ExecutionContext) extends ParsedCloudFormationCustomResourceRequestHandler {
  import Functor._
  import com.dwolla.cloudflare.model.Implicits._

  protected implicit val formats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
  protected lazy val logger: Logger = LoggerFactory.getLogger("LambdaLogger")

  private[record] val promisedCloudflareApiExecutor = Promise[CloudflareApiExecutor]

  protected lazy val kmsDecrypter = new KmsDecrypter

  protected def cloudFlareApiExecutor(email: String, key: String): Future[CloudflareApiExecutor] = {
    promisedCloudflareApiExecutor.success(new CloudflareApiExecutor(CloudflareAuthorization(email, key)))

    promisedCloudflareApiExecutor.future
  }

  protected def cloudFlareApiClient(executor: CloudflareApiExecutor) = new DnsRecordClient(executor)

  override def handleRequest(input: CloudFormationCustomResourceRequest): Future[HandlerResponse] = {
    val resourceProperties = input.ResourceProperties.getOrElse(throw MissingResourceProperties)
    val dnsRecord = parseDtoFrom(input.PhysicalResourceId, resourceProperties)

    decryptCloudflareCredentials(resourceProperties).flatMap { implicit cloudflare ⇒
      input.RequestType.toUpperCase match {
        case "CREATE" | "UPDATE" ⇒ handleCreateOrUpdate(dnsRecord, input.PhysicalResourceId)
        case "DELETE" ⇒ handleDelete(input.PhysicalResourceId.get)
      }
    }
  }

  private def decryptCloudflareCredentials(resourceProperties: Map[String, JValue]): Future[DnsRecordClient] = {
    val email = "CloudflareEmail"
    val key = "CloudflareKey"

    kmsDecrypter.decryptBase64(email → resourceProperties(email), key → resourceProperties(key))
      .map(byteArrays ⇒ byteArrays.mapValues(new String(_, "UTF-8")))
      .flatMap(cloudflareCredentials ⇒ cloudFlareApiExecutor(cloudflareCredentials(email), cloudflareCredentials(key)))
      .map(cloudFlareApiClient)
  }

  private def parseDtoFrom(id: Option[String], resourceProperties: Map[String, JValue]) = {
    val ttl: Option[String] = resourceProperties.get("TTL")
    val proxied: Option[String] = resourceProperties.get("Proxied")

    DnsRecordDTO(
      id = id,
      name = resourceProperties("Name"),
      content = resourceProperties("Content"),
      `type` = resourceProperties("Type"),
      ttl = ttl.map(_.toInt),
      proxied = proxied.map(_.toBoolean)
    )
  }

  override def shutdown(): Unit = {
    promisedCloudflareApiExecutor.future.map(_.close())
  }

  private def handleCreateOrUpdate(dnsRecordDto: DnsRecordDTO, cloudformationProvidedPhysicalResourceId: Option[String])
                                  (implicit cloudflare: DnsRecordClient): Future[HandlerResponse] = {

    for {
      existingRecord ← cloudflare.getExistingDnsRecord(dnsRecordDto.name)
      updateableId = existingRecord.map(_.physicalResourceId)
      createOrUpdate ← updateableId.fold[Future[CreateOrUpdate[IdentifiedDnsRecord]]](cloudflare.createDnsRecord(dnsRecordDto).map(Create(_))) { physicalResourceId ⇒
        val newRecordState = dnsRecordDto.identifyAs(physicalResourceId)
        assertRecordTypeWillNotChange(existingRecord.get.recordType, newRecordState.recordType)
        cloudflare.updateDnsRecord(newRecordState).map(Update(_))
      }
    } yield {
      warnIfProvidedIdDoesNotMatchDiscoveredId(cloudformationProvidedPhysicalResourceId, updateableId, dnsRecordDto.name)
      warnIfNoIdWasProvidedButDnsRecordExisted(cloudformationProvidedPhysicalResourceId, existingRecord)

      val dnsRecord = createOrUpdate.value
      val data = Map(
        "dnsRecord" → dnsRecord,
        "created" → createOrUpdate.create,
        "updated" → createOrUpdate.update,
        "oldDnsRecord" → existingRecord
      )

      HandlerResponse(dnsRecord.physicalResourceId, data)
    }
  }

  private def assertRecordTypeWillNotChange(existingRecord: String, newRecordState: String) = {
    if (existingRecord != newRecordState) throw DnsRecordTypeChange(existingRecord, newRecordState)
  }

  private def warnIfProvidedIdDoesNotMatchDiscoveredId(physicalResourceId: Option[String], updateableId: Option[String], hostname: String): Unit = {
    physicalResourceId.foreach { providedId =>
      updateableId.foreach { discoveredId =>
        if (providedId != discoveredId)
          logger.warn(s"""The passed physical ID "$providedId" does not match the discovered physical ID "$discoveredId" for hostname "$hostname". This may indicate a change to this stack's DNS entries that was not managed by CloudFormation. Updating the discovered record instead of the record passed by CloudFormation.""")
      }
    }
  }

  private def warnIfNoIdWasProvidedButDnsRecordExisted(physicalResourceId: Option[String], existingRecord: Option[DnsRecord]): Unit = {
    physicalResourceId.orElse {
      existingRecord.foreach { dnsRecord ⇒
        dnsRecord.id.foreach { discoveredId ⇒
          logger.warn(s"""Discovered DNS record ID "$discoveredId" for hostname "${dnsRecord.name}", with existing content "${dnsRecord.content}". This record will be updated instead of creating a new record.""")
        }
      }

      None
    }
  }

  private def handleDelete(physicalResourceId: String)
                          (implicit cloudflare: DnsRecordClient): Future[HandlerResponse] = {
    for {
      deleted ← cloudflare.deleteDnsRecord(physicalResourceId)
    } yield {
      val data = Map(
        "deletedRecordId" → deleted
      )

      HandlerResponse(physicalResourceId, data)
    }
  }.recover {
    case ex: DnsRecordIdDoesNotExistException ⇒
      logger.error("The record could not be deleted because it did not exist; nonetheless, responding with Success!", ex)
      HandlerResponse(physicalResourceId, Map.empty[String, AnyRef])
  }

  implicit def jvalueToString(jvalue: JValue): String = jvalue.extract[String]
}

object CloudflareDnsRecordHandler {
  val timeout: Duration = 30 seconds
}

case class DnsRecordTypeChange(existingRecordType: String, newRecordType: String)
  extends RuntimeException(s"""Refusing to change DNS record from "$existingRecordType" to "$newRecordType".""")
