package dwolla.cloudflare

import java.time.Instant

import cats.effect._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.logpush.{CreateJob, LogpushJob, LogpushOwnership}
import com.dwolla.cloudflare.domain.model.{LogpullOptionsTag, LogpushDestinationTag, LogpushIdTag, ZoneIdTag}
import io.circe.literal._
import org.http4s.client.Client
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import shapeless.tag.@@

class LogpushClientSpec extends Specification {
  def tagString[T](s: String): String @@ T = shapeless.tag[T][String](s)

  def tagInt[T](i: Int): Int @@ T = shapeless.tag[T][Int](i)

  trait Setup extends Scope {
    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    val zoneId = tagString[ZoneIdTag]("fake-zone-id")
    val logpushId1 = tagInt[LogpushIdTag](1)
    val logpushId2 = tagInt[LogpushIdTag](2)
    val destination1 = tagString[LogpushDestinationTag]("s3://cloudflare/logs/{DATE}?region=us-west-2&sse=AES256")
    val destination2 = tagString[LogpushDestinationTag]("s3://cloudflare")
    val options = tagString[LogpullOptionsTag]("fields=ClientIP&timestamps=rfc3339")
    val ts = Instant.parse("2018-11-27T19:10:00Z")
  }

  "list" should {
    "return all jobs" in new Setup {
      val http4sClient = fakeService.client(fakeService.listLogpushJobs(zoneId, SampleResponses.Successes.list))
      val client = buildClient(http4sClient, authorization)

      val output: List[LogpushJob] = client.list(zoneId).compile.toList.unsafeRunSync()
      output must be_==(
        List(
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
      )
    }
  }

  "createOwnership" should {
    "create new ownership" in new Setup {
      val http4sClient = fakeService.client(fakeService.createLogpushOwnership(zoneId, SampleResponses.Successes.createOwnership))
      val client = buildClient(http4sClient, authorization)

      val output = client.createOwnership(zoneId, destination1).compile.toList.unsafeRunSync()
      output must contain(
        LogpushOwnership(
          filename = "logs/20180101/cloudflare-ownership-challenge-11111111.txt",
          message = "",
          valid = true
        )
      )
    }

    "throw unexpected exception if error" in new Setup {
      val http4sClient = fakeService.client(fakeService.createLogpushOwnership(zoneId, SampleResponses.Failures.createOwnershipError))
      val client = buildClient(http4sClient, authorization)

      val output = client.createOwnership(zoneId, destination1)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException => ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(400),error parsing input: invalid json)
            |     """.stripMargin
      }
    }
  }

  "createJob" should {
    "create new job" in new Setup {
      val job = CreateJob(destination1, "challenge", None, None, None)
      val http4sClient = fakeService.client(fakeService.createLogpushJob(zoneId, SampleResponses.Successes.createJob))
      val client = buildClient(http4sClient, authorization)

      val output = client.createJob(zoneId, job).compile.toList.unsafeRunSync()
      output must contain(
        LogpushJob(
          id = logpushId1,
          enabled = true,
          name = None,
          logpullOptions = Some(options),
          destinationConf = destination1,
          lastComplete = None,
          lastError = None,
          errorMessage = None
        )
      )
    }

    "throw unexpected exception if error" in new Setup {
      val job = CreateJob(destination1, "challenge", None, None, None)
      val http4sClient = fakeService.client(fakeService.createLogpushJob(zoneId, SampleResponses.Failures.createJobError))
      val client = buildClient(http4sClient, authorization)

      val output = client.createJob(zoneId, job)
        .compile
        .toList
        .attempt
        .unsafeRunSync()

      output must beLeft[Throwable].like {
        case ex: UnexpectedCloudflareErrorException => ex.getMessage must_==
          """An unexpected Cloudflare error occurred. Errors:
            |
            | - Error(Some(400),new job not allowed)
            |     """.stripMargin
      }
    }
  }

  def buildClient[F[_] : Sync](http4sClient: Client[F], authorization: CloudflareAuthorization): LogpushClient[F] = {
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
