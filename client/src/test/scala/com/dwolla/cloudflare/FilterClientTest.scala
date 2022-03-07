package com.dwolla.cloudflare

import cats.effect._
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.filters._
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.{IOMatchers, Matchers}
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class FilterClientTest extends Specification with ScalaCheck with IOMatchers with Matchers {

  trait Setup extends Scope {
    val zoneId: ZoneId = tagZoneId("zone-id")
    val filterId = tagFilterId("d5266c8daa9443e081e5207f64763836")

    val authorization = CloudflareAuthorization("email", "key")
    val fakeService = new FakeCloudflareService(authorization)

    protected def buildFilterClient(service: HttpRoutes[IO]): FilterClient[IO] =
      FilterClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))

  }

  "list" should {

    "list the filters for the given zone" in new Setup {
      private val client = buildFilterClient(fakeService.listFilters(zoneId))
      private val output = client.list(zoneId)

      output.compile.toList must returnValue(containTheSameElementsAs(List(
        Filter(
          id = Option("97a013e8b34b4909bd454d84dccc6d02").map(tagFilterId),
          paused = false,
          description = Option("filter1"),
          ref = Option("ref1").map(tagFilterRef),
          expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
        ),
        Filter(
          id = Option("1e9746e64ff54e82b0c7306a2f93c1c6").map(tagFilterId),
          paused = false,
          expression = tagFilterExpression("(ip.src ne 0.0.0.0)"),
        )
      )))
    }
  }

  "get by id" should {

    "return the filter with the given id" in new Setup {
      private val client = buildFilterClient(fakeService.getFilterById(zoneId, filterId))
      private val output = client.getById(zoneId, filterId: String)

      output.compile.toList must returnValue(List(Filter(
        id = Option("97a013e8b34b4909bd454d84dccc6d02").map(tagFilterId),
        paused = false,
        description = Option("filter1"),
        ref = Option("ref1").map(tagFilterRef),
        expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
      )))
    }

  }

  "create" should {
    val input = Filter(
        expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
        paused = false
      )

    "send the json object and return its value" in new Setup {
      private val client = buildFilterClient(fakeService.createFilter(zoneId, filterId))
      private val output = client.create(zoneId, input)

      output.compile.toList must returnValue(List(input.copy(
        id = Option(filterId)
      )))
    }

    "raise a reasonable error if Cloudflare's business rules are violated" in new Setup {
      private val client = buildFilterClient(fakeService.createFilterFails)
      private val output = client.create(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(
          UnexpectedCloudflareErrorException(
            List(Error(None, "config duplicates an already existing config"))
          )
        )
      ))
    }
  }

  "update" should {
    "update the given filter" in new Setup {
      private val input = Filter(
        id = Option(filterId),
        expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
        paused = false
      )

      private val client = buildFilterClient(fakeService.updateFilter(zoneId, filterId))
      private val output = client.update(zoneId, input)

      output.compile.toList must returnValue(List(input))
    }

    "raise an exception when trying to update an unidentified filter" in new Setup {
      private val input = Filter(
          id = None,
          expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
          paused = false
        )

      private val client = buildFilterClient(fakeService.updateFilter(zoneId, filterId))
      private val output = client.update(zoneId, input)

      output.attempt.compile.toList must returnValue(List(
        Left(CannotUpdateUnidentifiedFilter(input))
      ))
    }
  }

  "delete" should {
    "delete the given filter" in new Setup {
      private val client = buildFilterClient(fakeService.deleteFilter(zoneId, filterId))
      private val output = client.delete(zoneId, filterId)

      output.compile.toList must returnValue(List(filterId))
    }

    "return success if the filter id doesn't exist" in new Setup {
      private val client = buildFilterClient(fakeService.deleteFilterThatDoesNotExist(zoneId, filterId, true))
      private val output = client.delete(zoneId, filterId)

      output.compile.toList must returnValue(List(filterId))
    }

    "return success if the filter id is invalid" in new Setup {
      private val client = buildFilterClient(fakeService.deleteFilterThatDoesNotExist(zoneId, filterId, false))
      private val output = client.delete(zoneId, filterId)

      output.compile.toList must returnValue(List(filterId))
    }
  }

  "buildUri and parseUri" should {
    val nonEmptyAlphaNumericString = Gen.identifier
    implicit val arbitraryZoneId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[ZoneIdTag][String]))
    implicit val arbitraryFilterId = Arbitrary(nonEmptyAlphaNumericString.map(shapeless.tag[FilterIdTag][String]))

    "be the inverse of each other" >> { prop { (zoneId: ZoneId, filterId: FilterId) =>
      val client = new FilterClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, Filter] = ???
        override def getById(zoneId: ZoneId, filterId: String): fs2.Stream[IO, Filter] = ???
        override def create(zoneId: ZoneId, filter: Filter): fs2.Stream[IO, Filter] = ???
        override def update(zoneId: ZoneId, filter: Filter): fs2.Stream[IO, Filter] = ???
        override def delete(zoneId: ZoneId, filterId: String): fs2.Stream[IO, FilterId] = ???
      }

      client.parseUri(client.buildUri(zoneId, filterId).renderString) must beSome((zoneId, filterId))
    }}
  }
}
