package com.dwolla.cloudflare

import cats.effect.*
import com.dwolla.cloudflare.domain.model.Exceptions.UnexpectedCloudflareErrorException
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.cloudflare.domain.model.filters.*
import dwolla.cloudflare.FakeCloudflareService
import org.http4s.HttpRoutes
import org.scalacheck.{Arbitrary, Gen}
import munit.CatsEffectSuite
import munit.ScalaCheckSuite

class FilterClientTest extends CatsEffectSuite with ScalaCheckSuite {

  // Common setup values and helper
  val zoneId: ZoneId = tagZoneId("zone-id")
  val filterId: FilterId = tagFilterId("d5266c8daa9443e081e5207f64763836")

  val authorization = CloudflareAuthorization("email", "key")

  private def buildFilterClient(service: HttpRoutes[IO]): FilterClient[IO] = {
    val fakeService = new FakeCloudflareService(authorization)
    FilterClient(new StreamingCloudflareApiExecutor(fakeService.client(service), authorization))
  }

  test("list should list the filters for the given zone") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.listFilters(zoneId))
    val output = client.list(zoneId)

    val expected = List(
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
    )

    assertIO(output.compile.toList, expected)
  }

  test("get by id should return the filter with the given id") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.getFilterById(zoneId, filterId))
    val output = client.getById(zoneId, filterId.value)

    val expected = List(Filter(
      id = Option("97a013e8b34b4909bd454d84dccc6d02").map(tagFilterId),
      paused = false,
      description = Option("filter1"),
      ref = Option("ref1").map(tagFilterRef),
      expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
    ))

    assertIO(output.compile.toList, expected)
  }

  private val createInput = Filter(
    expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
    paused = false
  )

  test("create should send the json object and return its value") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.createFilter(zoneId, filterId))
    val output = client.create(zoneId, createInput)

    val expected = List(createInput.copy(
      id = Option(filterId)
    ))

    assertIO(output.compile.toList, expected)
  }

  test("create should raise a reasonable error if Cloudflare's business rules are violated") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.createFilterFails)
    val output = client.create(zoneId, createInput)

    val expected = List(
      Left(
        UnexpectedCloudflareErrorException(
          List(Error(None, "config duplicates an already existing config"))
        )
      )
    )

    assertIO(output.attempt.compile.toList, expected)
  }

  test("update should update the given filter") {
    val input = Filter(
      id = Option(filterId),
      expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
      paused = false
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.updateFilter(zoneId, filterId))
    val output = client.update(zoneId, input)

    val expected = List(input)

    assertIO(output.compile.toList, expected)
  }

  test("update should raise an exception when trying to update an unidentified filter") {
    val input = Filter(
      id = None,
      expression = tagFilterExpression("(cf.bot_management.verified_bot)"),
      paused = false
    )

    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.updateFilter(zoneId, filterId))
    val output = client.update(zoneId, input)

    val expected = List(Left(CannotUpdateUnidentifiedFilter(input)))

    assertIO(output.attempt.compile.toList, expected)
  }

  test("delete should delete the given filter") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.deleteFilter(zoneId, filterId))
    val output = client.delete(zoneId, filterId.value)

    val expected = List(filterId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the filter id doesn't exist") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.deleteFilterThatDoesNotExist(zoneId, filterId, true))
    val output = client.delete(zoneId, filterId.value)

    val expected = List(filterId)
    assertIO(output.compile.toList, expected)
  }

  test("delete should return success if the filter id is invalid") {
    val fakeService = new FakeCloudflareService(authorization)
    val client = buildFilterClient(fakeService.deleteFilterThatDoesNotExist(zoneId, filterId, false))
    val output = client.delete(zoneId, filterId.value)

    val expected = List(filterId)
    assertIO(output.compile.toList, expected)
  }

  // property-based: buildUri and parseUri are inverses
  private val nonEmptyAlphaNumericString = Gen.identifier
  implicit private val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(nonEmptyAlphaNumericString.map(ZoneId(_)))
  implicit private val arbitraryFilterId: Arbitrary[FilterId] = Arbitrary(nonEmptyAlphaNumericString.map(FilterId(_)))

  property("buildUri and parseUri should be inverses") {
    import org.scalacheck.Prop.forAll

    forAll { (zoneId: ZoneId, filterId: FilterId) =>
      val client = new FilterClient[IO] {
        override def list(zoneId: ZoneId): fs2.Stream[IO, Filter] = ???
        override def getById(zoneId: ZoneId, filterId: String): fs2.Stream[IO, Filter] = ???
        override def create(zoneId: ZoneId, filter: Filter): fs2.Stream[IO, Filter] = ???
        override def update(zoneId: ZoneId, filter: Filter): fs2.Stream[IO, Filter] = ???
        override def delete(zoneId: ZoneId, filterId: String): fs2.Stream[IO, FilterId] = ???
      }

      assertEquals(client.parseUri(client.buildUri(zoneId, filterId).renderString), Some((zoneId, filterId)))
    }
  }
}
