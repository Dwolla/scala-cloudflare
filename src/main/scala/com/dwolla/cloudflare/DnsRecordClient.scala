package com.dwolla.cloudflare

import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.domain.model.{DnsRecordDTO, Error, IdentifiedDnsRecord, UnidentifiedDnsRecord}
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{HttpDelete, HttpGet, HttpPost, HttpPut}
import org.json4s.native._
import org.json4s.{DefaultFormats, Formats, MonadicJValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class DnsRecordClient(executor: CloudflareApiExecutor)(implicit val ec: ExecutionContext) {

  import com.dwolla.cloudflare.domain.model.Implicits._

  protected implicit val formats: Formats = DefaultFormats

  def createDnsRecord(record: UnidentifiedDnsRecord): Future[IdentifiedDnsRecord] = {
    getZoneId(domainNameToZoneName(record.name)).flatMap { zoneId ⇒
      val request = new HttpPost(s"https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records")
      request.setEntity(record)

      executor.fetch(request) { response ⇒
        (parseJson(response.getEntity.getContent) \ "result").extract[DnsRecordDTO]
      }.map((_: DnsRecordDTO, zoneId))
    }
  }

  def updateDnsRecord(record: IdentifiedDnsRecord): Future[IdentifiedDnsRecord] = {
    val request = new HttpPut(record.physicalResourceId)
    request.setEntity(record.unidentify)

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result").extract[DnsRecordDTO]
    }.map((_, record.zoneId))
  }

  def getExistingDnsRecord(name: String, content: Option[String]=None, recordType: Option[String]=None): Future[Option[IdentifiedDnsRecord]] = {
    getZoneId(domainNameToZoneName(name)).flatMap { zoneId ⇒
      val parameters = Seq(Option("name" → name), content.map("content" → _), recordType.map("type" → _))
        .collect {
          case Some((key, value)) ⇒ s"$key=$value"
        }
        .mkString("&")
      val request: HttpGet = new HttpGet(s"https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records?$parameters")

      executor.fetch(request) { response ⇒
        val records = (response \ "result").extract[Set[DnsRecordDTO]]
        if (records.size > 1) throw MultipleCloudflareRecordsExistForDomainNameException(name, records)
        records.headOption.flatMap { dto ⇒
          dto.id.map(dto.identifyAs(zoneId, _))
        }
      }
    }
  }

  def deleteDnsRecord(physicalResourceId: String): Future[String] = {
    val request = new HttpDelete(physicalResourceId)

    executor.fetch(request) { response ⇒
      val parsedJson = parseJson(response.getEntity.getContent)

      response.getStatusLine.getStatusCode match {
        case statusCode if (200 to 299) contains statusCode ⇒
          (parsedJson \ "result" \ "id").extract[String]
        case 400 ⇒
          val errors = (parsedJson \ "errors").extract[List[Error]]

          if (errors.contains(Error(1032, "Invalid DNS record identifier")) && errors.length == 1)
            throw DnsRecordIdDoesNotExistException(physicalResourceId)
          else
            throw UnexpectedCloudflareErrorException(errors)
        case _ ⇒
          throw UnexpectedCloudflareErrorException((parsedJson \ "errors").extract[List[Error]])
      }
    }
  }

  def getZoneId(domain: String): Future[String] = {
    val request = new HttpGet(s"https://api.cloudflare.com/client/v4/zones?name=$domain&status=active")

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result" \ "id") (0).extract[String]
    }
  }

  private def domainNameToZoneName(name: String): String = name.split('.').takeRight(2).mkString(".")

  implicit def httpResponseToJsonInput(httpResponse: HttpResponse): MonadicJValue = parseJson(httpResponse.getEntity.getContent)
}

case class MultipleCloudflareRecordsExistForDomainNameException(domainName: String, records: Set[DnsRecordDTO]) extends RuntimeException(
  s"""Multiple DNS records exist for domain name $domainName:
     |
     | - ${records.mkString("\n - ")}
     |
     |This resource refuses to process multiple records because the intention is not clear.
     |Clean up the records manually or provide additional parameters to filter on.""".stripMargin)

case class DnsRecordIdDoesNotExistException(resourceId: String) extends RuntimeException(
  s"The given DNS record ID does not exist ($resourceId)."
)

case class UnexpectedCloudflareErrorException(errors: List[Error]) extends RuntimeException(
  s"""An unexpected Cloudflare error occurred. Errors:
     |
     | - ${errors.mkString("\n - ")}
   """.stripMargin
)
