package com.dwolla.cloudflare.clients

import cats._
import cats.implicits._
import com.dwolla.cloudflare.CloudflareApiExecutor
import com.dwolla.cloudflare.common.JsonEntity._
import com.dwolla.cloudflare.common.UriHelper
import com.dwolla.cloudflare.domain.dto.ResponseDTO
import com.dwolla.cloudflare.domain.dto.dns.DnsRecordDTO
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.dns.{IdentifiedDnsRecord, UnidentifiedDnsRecord}
import com.dwolla.cloudflare.domain.model.{DeletedRecord, Error}
import org.apache.http.client.methods._
import org.json4s.native._
import org.json4s.{DefaultFormats, Formats}

import scala.language.{higherKinds, implicitConversions}

class DnsRecordClient[F[_] : Monad](executor: CloudflareApiExecutor[F]) {

  import com.dwolla.cloudflare.domain.model.dns.Implicits._
  import com.dwolla.cloudflare.domain.model.response.Implicits._

  protected implicit val formats: Formats = DefaultFormats

  def createRecord(record: UnidentifiedDnsRecord): F[IdentifiedDnsRecord] = {
    getZoneId(domainNameToZoneName(record.name)).flatMap { zoneId ⇒
      val request = new HttpPost(UriHelper.buildApiUri(s"zones/$zoneId/dns_records"))
      request.setEntity(record)

      executor.fetch(request) { response ⇒
        parseJson(response.getEntity.getContent).extract[ResponseDTO[DnsRecordDTO]].result
      }.map((_: DnsRecordDTO, zoneId))
    }
  }

  def updateRecord(record: IdentifiedDnsRecord): F[IdentifiedDnsRecord] = {
    val request = new HttpPut(record.physicalResourceId)
    request.setEntity(record.unidentify)

    executor.fetch(request) { response ⇒
      parseJson(response.getEntity.getContent).extract[ResponseDTO[DnsRecordDTO]].result
    }.map((_, record.zoneId))
  }

  def getExistingRecord(name: String, content: Option[String] = None, recordType: Option[String] = None): F[Option[IdentifiedDnsRecord]] = {
    getZoneId(domainNameToZoneName(name)).flatMap { zoneId ⇒
      val parameters = UriHelper.buildParameterString(Seq(Option("name" → name), content.map("content" → _), recordType.map("type" → _)))
      val request: HttpGet = new HttpGet(UriHelper.buildApiUri(s"zones/$zoneId/dns_records", Some(parameters)))

      executor.fetch(request) { response ⇒
        val records = parseJson(response.getEntity.getContent).extract[ResponseDTO[Set[DnsRecordDTO]]].result
        if (records.size > 1) throw MultipleCloudflareRecordsExistForDomainNameException(name, records)
        records.headOption.flatMap { dto ⇒
          dto.id.map(dto.identifyAs(zoneId, _))
        }
      }
    }
  }

  def getExistingRecordsWithContentFilter(name: String, contentPredicate: String ⇒ Boolean, recordType: Option[String] = None): F[Set[IdentifiedDnsRecord]] = {
    getZoneId(domainNameToZoneName(name)).flatMap { zoneId ⇒
      val parameters = UriHelper.buildParameterString(Seq(Option("name" → name), recordType.map("type" → _)))
      val request: HttpGet = new HttpGet(UriHelper.buildApiUri(s"zones/$zoneId/dns_records", Some(parameters)))

      executor.fetch(request) { response ⇒
        val records = parseJson(response.getEntity.getContent).extract[ResponseDTO[Set[DnsRecordDTO]]].result
        val filteredRecords = records.filter(r ⇒ contentPredicate(r.content))
        filteredRecords.flatMap { dto ⇒
          dto.id.map(dto.identifyAs(zoneId, _))
        }
      }
    }
  }

  def deleteRecord(physicalResourceId: String): F[String] = {
    val request = new HttpDelete(physicalResourceId)

    executor.fetch(request) { response ⇒
      val r = parseJson(response.getEntity.getContent).extract[ResponseDTO[DeletedRecord]]

      response.getStatusLine.getStatusCode match {
        case statusCode if (200 to 299) contains statusCode ⇒
          r.result.id
        case 400 ⇒
          val errors = toErrorList(r.errors.get)

          if (errors.contains(Error(1032, "Invalid DNS record identifier")) && errors.length == 1)
            throw DnsRecordIdDoesNotExistException(physicalResourceId)
          else
            throw UnexpectedCloudflareErrorException(errors)
        case _ ⇒
          throw UnexpectedCloudflareErrorException(r.errors.get)
      }
    }
  }

  def getZoneId(domain: String): F[String] = {
    val parameters = UriHelper.buildParameterString(Seq(Option("name" → domain), Option("status" → "active")))
    val request = new HttpGet(UriHelper.buildApiUri(s"zones", Some(parameters)))

    executor.fetch(request) { response ⇒
      (parseJson(response.getEntity.getContent) \ "result" \ "id") (0).extract[String]
    }
  }

  private def domainNameToZoneName(name: String): String = name.split('.').takeRight(2).mkString(".")
}

case class MultipleCloudflareRecordsExistForDomainNameException(domainName: String, records: Set[DnsRecordDTO]) extends RuntimeException(
  s"""Multiple DNS records exist for domain name $domainName:
     |
     | - ${records.mkString("\n - ")}
     |
     |This resource refuses to process multiple records because the intention is not clear.
     |Clean up the records manually or provide additional parameters to filter on.""".stripMargin)

case class DnsRecordIdDoesNotExistException(resourceId: String) extends RuntimeException(
  s"The given DNS record ID does not exist ($resourceId).")