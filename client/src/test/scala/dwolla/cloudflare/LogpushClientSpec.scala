package dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.logpush.{CreateJob, LogpushJob, LogpushOwnership}
import io.circe.literal.*
import munit.CatsEffectSuite
import org.http4s.client.Client

import java.time.Instant

class LogpushClientSpec extends CatsEffectSuite {
  // Common setup
  private val authorization = CloudflareAuthorization("email", "key")
  private val fakeService = new FakeCloudflareService(authorization)

  private val zoneId = ZoneId("fake-zone-id")
  private val logpushId1 = LogpushId(1)
  private val logpushId2 = LogpushId(2)
  private val destination1 = LogpushDestination("s3://cloudflare/logs/{DATE}?region=us-west-2&sse=AES256")
  private val destination2 = LogpushDestination("s3://cloudflare")
  private val options = LogpullOptions("fields=ClientIP&timestamps=rfc3339")
  private val ts = Instant.parse("2018-11-27T19:10:00Z")

  test("list should return all jobs") {
    val http4sClient = fakeService.client(fakeService.listLogpushJobs(zoneId, SampleResponses.Successes.list))
    val client = buildClient(http4sClient, authorization)

    val output = client
      .list(zoneId)
      .compile
      .toList

    val expected = List(
      LogpushJob(
        id = logpushId1,
        enabled = true,
        name = Some("Job 1"),
        logpullOptions = Some(options),
        destinationConf = destination1,
        lastComplete = Some(ts),
        lastError = None,
        errorMessage = None
      ),
      LogpushJob(
        id = logpushId2,
        enabled = false,
        name = None,
        logpullOptions = None,
        destinationConf = destination2,
        lastComplete = None,
        lastError = Some(ts),
        errorMessage = Some("error")
      )
    )

    assertIO(output, expected)
  }

  test("createOwnership should create new ownership") {
    val http4sClient = fakeService.client(fakeService.createLogpushOwnership(zoneId, SampleResponses.Successes.createOwnership))
    val client = buildClient(http4sClient, authorization)

    val output = client
      .createOwnership(zoneId, destination1)
      .compile
      .toList

    val expected = LogpushOwnership(
      filename = "logs/20180101/cloudflare-ownership-challenge-11111111.txt",
      message = "",
      valid = true
    )

    assertIO(output.map(_.contains(expected)), true)
  }

  test("createOwnership should throw unexpected exception if error") {
    val http4sClient = fakeService.client(fakeService.createLogpushOwnership(zoneId, SampleResponses.Failures.createOwnershipError))
    val client = buildClient(http4sClient, authorization)

    val io = client.createOwnership(zoneId, destination1)
      .compile
      .toList

    interceptIO[UnexpectedCloudflareErrorException](io).map { ex =>
      assertEquals(ex.getMessage,
        """An unexpected Cloudflare error occurred. Errors:
          |
          | - Error(Some(400),error parsing input: invalid json)
          |     """.stripMargin)
    }
  }

  test("createJob should create new job") {
    val job = CreateJob(destination1, "challenge", None, None, None)
    val http4sClient = fakeService.client(fakeService.createLogpushJob(zoneId, SampleResponses.Successes.createJob))
    val client = buildClient(http4sClient, authorization)

    val output = client
      .createJob(zoneId, job)
      .compile
      .toList

    val expected = LogpushJob(
      id = logpushId1,
      enabled = true,
      name = None,
      logpullOptions = Some(options),
      destinationConf = destination1,
      lastComplete = None,
      lastError = None,
      errorMessage = None
    )

    assertIO(output.map(_.contains(expected)), true)
  }

  test("createJob should throw unexpected exception if error") {
    val job = CreateJob(destination1, "challenge", None, None, None)
    val http4sClient = fakeService.client(fakeService.createLogpushJob(zoneId, SampleResponses.Failures.createJobError))
    val client = buildClient(http4sClient, authorization)

    val io = client
      .createJob(zoneId, job)
      .compile
      .toList

    interceptIO[UnexpectedCloudflareErrorException](io).map { ex =>
      assertEquals(ex.getMessage,
        """An unexpected Cloudflare error occurred. Errors:
          |
          | - Error(Some(400),new job not allowed)
          |     """.stripMargin)
    }
  }

  def buildClient[F[_] : Concurrent](http4sClient: Client[F], authorization: CloudflareAuthorization): LogpushClient[F] = {
    val fakeHttp4sExecutor = new StreamingCloudflareApiExecutor(http4sClient, authorization)
    LogpushClient(fakeHttp4sExecutor)
  }

  private object SampleResponses {
    object Successes {
      val list =
        json"""{
            "errors": [],
            "messages": [],
            "result": [
              {
                "id": 1,
                "enabled": true,
                "name": "Job 1",
                "logpull_options": "fields=ClientIP&timestamps=rfc3339",
                "destination_conf": "s3://cloudflare/logs/{DATE}?region=us-west-2&sse=AES256",
                "last_complete": "2018-11-27T19:10:00Z",
                "last_error": null,
                "error_message": null
              },
              {
                "id": 2,
                "enabled": false,
                "name": null,
                "logpull_options": null,
                "destination_conf": "s3://cloudflare",
                "last_complete": null,
                "last_error": "2018-11-27T19:10:00Z",
                "error_message": "error"
              }
            ],
            "success": true
          }
        """

      val createOwnership =
        json"""{
            "errors": [],
            "messages": [],
            "result": {
              "filename": "logs/20180101/cloudflare-ownership-challenge-11111111.txt",
              "message": "",
              "valid": true
            },
            "success": true
          }"""

      val createJob =
        json"""{
            "errors": [],
            "messages": [],
            "result": {
              "id": 1,
              "enabled": true,
              "name": null,
              "logpull_options": "fields=ClientIP&timestamps=rfc3339",
              "destination_conf": "s3://cloudflare/logs/{DATE}?region=us-west-2&sse=AES256",
              "last_complete": null,
              "last_error": null,
              "error_message": null
            },
            "success": true
          }"""
    }

    object Failures {
      val createOwnershipError =
        json"""{
            "success": false,
            "errors": [
              {
                "code": 400,
                "message": "error parsing input: invalid json"
              }
            ],
            "messages": [],
            "result": null
          }
        """

      val createJobError =
        json"""{
            "errors": [
            {
              "code": 400,
              "message": "new job not allowed"
            }
            ],
            "messages": [],
            "result": null,
            "success": false
          }
        """
    }
  }
}
