package dwolla.cloudflare

import java.time.Instant

import cats.data._
import cats.effect._
import com.dwolla.cloudflare._
import com.dwolla.cloudflare.domain.dto._
import com.dwolla.cloudflare.domain.dto.dns.DnsRecordDTO
import com.dwolla.cloudflare.domain.model.ZoneSettings.CloudflareSettingValue
import com.dwolla.cloudflare.domain.model._
import com.dwolla.cloudflare.domain.model.accesscontrolrules._
import com.dwolla.cloudflare.domain.model.firewallrules._
import com.dwolla.cloudflare.domain.model.pagerules.PageRule.pageRuleEncoder
import com.dwolla.cloudflare.domain.model.pagerules.PageRule.pageRuleDecoder
import com.dwolla.cloudflare.domain.model.pagerules._
import com.dwolla.cloudflare.domain.model.ratelimits._
import io.circe._
import io.circe.Encoder._
import io.circe.literal._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.syntax.all._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.VirtualHost
import org.http4s.server.middleware.VirtualHost.exact
import org.http4s.util.CaseInsensitiveString

class FakeCloudflareService(authorization: CloudflareAuthorization) extends Http4sDsl[IO] {

  object OptionalPageQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("page")
  object DirectionPageQueryParamMatcher extends QueryParamDecoderMatcher[String]("direction")
  object ListAccessControlRulesParameters {
    object modeParam extends QueryParamDecoderMatcher[String]("mode")
  }

  import com.dwolla.cloudflare.domain.model.Implicits._

  object ListZonesQueryParameters {
    object zoneName extends QueryParamDecoderMatcher[String]("name")
    object status extends QueryParamDecoderMatcher[String]("status")
  }

  object ListRecordsForZoneQueryParameters {
    object recordNameParam extends QueryParamDecoderMatcher[String]("name")
    object contentParam extends OptionalQueryParamDecoderMatcher[String]("content")
    object recordTypeParam extends OptionalQueryParamDecoderMatcher[String]("type")
  }

  def listZones(zoneName: String, responseBody: Json) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" :? ListZonesQueryParameters.zoneName(zone) +& ListZonesQueryParameters.status("active") if zone == zoneName =>
      Ok(responseBody)
    case GET -> Root / "client" / "v4" / "zones" =>
      Ok(
        json"""{
            "result": [],
            "result_info": {
              "page": 1,
              "per_page": 20,
              "total_pages": 1,
              "count": 0,
              "total_count": 0
            },
            "success": false,
            "errors": [],
            "messages": []
          }""")
  }

  val failure =
    Ok(json"""{
        "result": [],
        "result_info": {
          "page": 1,
          "per_page": 20,
          "total_pages": 1,
          "count": 0,
          "total_count": 0
        },
        "success": false,
        "errors": [{"code": 42, "message": "oops"}],
        "messages": []
      }""")

  def getDnsRecordByUri(fakeZoneId: String, fakeRecordId: String) = HttpRoutes.of[IO] {
    case req@GET -> Root / "client" / "v4" / "zones" / zoneId / "dns_records" / recordId if zoneId == fakeZoneId && recordId == fakeRecordId =>
      val record: DnsRecord = IdentifiedDnsRecord(
        physicalResourceId = shapeless.tag[PhysicalResourceIdTag][String](req.uri.toString()),
        zoneId = shapeless.tag[ZoneIdTag][String](fakeZoneId),
        resourceId = shapeless.tag[ResourceIdTag][String](fakeRecordId),
        name = "example.hydragents.xyz",
        content = "content.hydragents.xyz",
        recordType = "CNAME",
      )

      val responseBody: ResponseDTO[DnsRecordDTO] = ResponseDTO(
        result = record.toDto,
        success = true,
        errors = None,
        messages = None,
      )

      Ok(responseBody.asJson)
    case GET -> Root / "client" / "v4" / "zones" / zoneId / "dns_records" / recordId if zoneId != fakeZoneId || recordId != fakeRecordId =>
      BadRequest(ResponseDTO[None.type](
        result = None,
        success = false,
        errors = Option(List(
          ResponseInfoDTO(Option(7003), s"Could not route to /zones/$zoneId/dns_records/$recordId, perhaps your object identifier is invalid?"),
          ResponseInfoDTO(Option(7000), "No route for that URI")
        )),
        messages = None,
      ).asJson)
  }

  def listRecordsForZone(zoneId: String,
    recordName: String,
    responseBody: String,
    contentFilter: Option[String] = None,
    recordTypeFilter: Option[String] = None,
  ) = {
    import ListRecordsForZoneQueryParameters._
    HttpRoutes.of[IO] {
      case GET -> Root / "client" / "v4" / "zones" / zone / "dns_records" :? recordNameParam(name) +& contentParam(c) +& recordTypeParam(t)
        if zone == zoneId &&
          name == recordName &&
          c == contentFilter &&
          t == recordTypeFilter =>
        okWithJson(responseBody)
      case GET -> Root / "client" / "v4" / "zones" / _ => failure
    }
  }

  def createRecordInZone(zoneId: String) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "zones" / zone / "dns_records" if zone == zoneId =>
      for {
        dnsRecordDTO <- req.decodeJson[DnsRecordDTO]
        res <-
          if (dnsRecordDTO.id.isDefined) BadRequest()
          else {
            Created(ResponseDTO[DnsRecordDTO](
              result = dnsRecordDTO.copy(id = Option("fake-record-id"), ttl = dnsRecordDTO.ttl.orElse(Some(1))),
              success = true,
              errors = None,
              messages = None
            ).asJson)
          }
      } yield res
  }

  def createRecordThatAlreadyExists(zoneId: String) = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / zone / "dns_records" if zone == zoneId =>
      BadRequest(ResponseDTO[DnsRecordDTO](
        result = None,
        success = false,
        errors = Option(List(ResponseInfoDTO(code = Option(81057), message = "The record already exists."))),
        messages = None,
      ).asJson)
  }

  def updateRecordInZone(zoneId: String, recordId: String) = HttpRoutes.of[IO] {
    case req@PUT -> Root / "client" / "v4" / "zones" / zone / "dns_records" / record if zone == zoneId && record == recordId =>
      for {
        dnsRecordDTO <- req.decodeJson[DnsRecordDTO]
        res <-
          if (dnsRecordDTO.id.isDefined) BadRequest()
          else {
            Ok(ResponseDTO[DnsRecordDTO](
              result = dnsRecordDTO.copy(id = Option(recordId), ttl = dnsRecordDTO.ttl.orElse(Some(1)), proxied = dnsRecordDTO.proxied.orElse(Some(false))),
              success = true,
              errors = None,
              messages = None
            ).asJson)
          }
      } yield res
  }

  def deleteRecordInZone(zoneId: String,
    recordId: String,
  ) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zone / "dns_records" / record if zone == zoneId && record == recordId =>
      Ok(ResponseDTO[DeleteResult](
        result = DeleteResult(id = recordId),
        success = true,
        errors = None,
        messages = None
      ).asJson)
  }

  def failedDeleteRecordInZone(zoneId: String, recordId: String, responseBody: String) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zone / "dns_records" / record if zone == zoneId && record == recordId =>
      BadRequest(parseJson(responseBody))
  }

  def listRateLimits(zoneId: ZoneId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "rate_limits" if id == zoneId =>
      Ok(
        json"""{
                 "result": [
                    {
                      "id": "ec794f8d14e2407084de98f4a39e6387",
                      "disabled": true,
                      "description": "hydragents.xyz/sign-up",
                      "match": {
                        "request": {
                          "methods": [
                            "POST"
                          ],
                          "schemes": [
                            "_ALL_"
                          ],
                          "url": "hydragents.xyz/sign-up"
                        },
                        "response": {
                          "origin_traffic": true,
                          "headers": [
                            {
                              "name": "Cf-Cache-Status",
                              "op": "ne",
                              "value": "HIT"
                            }
                          ]
                        }
                      },
                      "login_protect": false,
                      "threshold": 5,
                      "period": 60,
                      "action": {
                        "mode": "challenge",
                        "timeout": 0
                      }
                    },
                    {
                      "id": "5e806af5c96b4e338a452b156fe8bcdb",
                      "disabled": true,
                      "description": "hydragents.xyz/sign-up",
                      "match": {
                        "request": {
                          "methods": [
                            "POST"
                          ],
                          "schemes": [
                            "_ALL_"
                          ],
                          "url": "hydragents.xyz/sign-up"
                        }
                      },
                      "login_protect": false,
                      "threshold": 20,
                      "period": 600,
                      "action": {
                        "mode": "challenge",
                        "timeout": 0
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": [],
                  "result_info": {
                    "page": 1,
                    "per_page": 20,
                    "count": 2,
                    "total_count": 2,
                    "total_pages": 1
                  }
                }"""
      )
  }

  def getRateLimitById(zoneId: ZoneId, rateLimitId: RateLimitId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / zid / "rate_limits" / rid if zid == zoneId && rid == rateLimitId =>
      Ok(
        json"""{
                "result": {
                  "id": "ec794f8d14e2407084de98f4a39e6387",
                  "disabled": true,
                  "description": "hydragents.xyz/sign-up",
                  "match": {
                    "request": {
                      "methods": [
                        "POST"
                      ],
                      "schemes": [
                        "_ALL_"
                      ],
                      "url": "hydragents.xyz/sign-up"
                    },
                    "response": {
                      "origin_traffic": true,
                      "headers": [
                        {
                          "name": "Cf-Cache-Status",
                          "op": "ne",
                          "value": "HIT"
                        }
                      ]
                    }
                  },
                  "login_protect": false,
                  "threshold": 5,
                  "period": 60,
                  "action": {
                    "mode": "challenge",
                    "timeout": 0
                  }
                },
                "success": true,
                "errors": [],
                "messages": []
               }"""
      )
  }

  def createRateLimit(zoneId: ZoneId, rateLimitId: RateLimitId) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "zones" / zid / "rate_limits" if zid == zoneId =>
      for {
        input <- req.decodeJson[RateLimit]
        created = input.copy(
          id = Option(rateLimitId),
        )
        resp <- Ok(ResponseDTO(
          result = created,
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      } yield resp
  }

  val createRateLimitFails = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / _ / "rate_limits" =>
      Ok(
        json"""{
                 "success": false,
                 "errors": [
                   {
                     "message": "ratelimit.api.validation_error:ratelimit.api.mitigation_timeout_must_be_greater_than_period"
                   }
                 ],
                 "messages": [],
                 "result": null
               }
               """)
  }

  def updateRateLimit(zoneId: ZoneId, rateLimitId: RateLimitId) = HttpRoutes.of[IO] {
    case req@PUT -> Root / "client" / "v4" / "zones" / zid / "rate_limits" / rid if zid == zoneId && rid == rateLimitId =>
      for {
        input <- req.decodeJson[RateLimit]
        resp <- if (input.id.isEmpty)
          Ok(ResponseDTO(
            result = input.copy(id = Option(rateLimitId)),
            success = true,
            errors = None,
            messages = None,
          ).asJson)
        else
          BadRequest("input ID should be empty")
      } yield resp
  }

  def deleteRateLimit(zoneId: ZoneId, rateLimitId: RateLimitId) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "rate_limits" / rid if zid == zoneId && rid == rateLimitId =>
      Ok(
        json"""{
                 "result": {
                   "id": ${rateLimitId: String}
                 },
                 "success": true,
                 "errors": [],
                 "messages": []
               }""")
  }

  def deleteRateLimitThatDoesNotExist(zoneId: ZoneId, validId: Boolean) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "rate_limits" / _ if zid == zoneId =>
      if (validId)
        Ok(
          json"""{
                   "success": false,
                   "errors": [
                     {
                       "code": 1000,
                       "message": "not_found"
                     }
                   ],
                   "messages": [],
                   "result": null
                 }""")
      else
        Ok(
          json"""{
                   "success": false,
                   "errors": [
                     {
                       "code": 7003,
                       "message": "Could not route to /zones/90940840480ba654a3a5ddcdc5d741f9/rate_limits/asdf, perhaps your object identifier is invalid?"
                     },
                     {
                       "code": 7000,
                       "message": "No route for that URI"
                     }
                   ],
                   "messages": [],
                   "result": null
                 }""")

  }

  def listAccessControlRulesByAccount(accountId: AccountId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / id / "firewall" / "access_rules" / "rules" if id == accountId =>
      Ok(
        json"""{
                 "result": [
                     {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "1.2.3.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Account",
                        "type": "account"
                      }
                    },
                    {
                      "id": "fake-access-rule-2",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "2.3.4.5"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Account",
                        "type": "account"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def listAccessControlRulesByZone(zoneId: ZoneId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "firewall" / "access_rules" / "rules" if id == zoneId =>
      Ok(
        json"""{
                 "result": [
                     {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "1.2.3.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Zone",
                        "type": "zone"
                      }
                    },
                    {
                      "id": "fake-access-rule-2",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "2.3.4.5"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Zone",
                        "type": "zone"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def listAccessControlRulesByAccountFilteredByWhitelistMode(accountId: AccountId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / id / "firewall" / "access_rules" / "rules" :? ListAccessControlRulesParameters.modeParam("whitelist") if id == accountId =>
      Ok(
        json"""{
                 "result": [
                    {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "whitelist",
                      "configuration": {
                        "target": "ip",
                        "value": "1.2.3.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Account",
                        "type": "account"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def listAccessControlRulesByZoneFilteredByWhitelistMode(zoneId: ZoneId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "firewall" / "access_rules" / "rules" :? ListAccessControlRulesParameters.modeParam("whitelist") if id == zoneId =>
      Ok(
        json"""{
                 "result": [
                    {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "whitelist",
                      "configuration": {
                        "target": "ip",
                        "value": "1.2.3.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Zone",
                        "type": "zone"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def getAccessControlRuleByIdForAccount(accountId: AccountId, ruleId: AccessControlRuleId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / id / "firewall" / "access_rules" / "rules" / rid if id == accountId && rid == ruleId =>
      Ok(
        json"""{
                 "result": [
                     {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "198.51.100.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Account",
                        "type": "account"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def getAccessControlRuleByIdForZone(zoneId: ZoneId, ruleId: AccessControlRuleId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "firewall" / "access_rules" / "rules" / rid if id == zoneId && rid == ruleId =>
      Ok(
        json"""{
                 "result": [
                     {
                      "id": "fake-access-rule-1",
                      "notes": "Some notes",
                      "allowed_modes": [
                        "whitelist",
                        "block",
                        "challenge",
                        "js_challenge"
                      ],
                      "mode": "challenge",
                      "configuration": {
                        "target": "ip",
                        "value": "198.51.100.4"
                      },
                      "created_on": "2014-01-01T05:20:00.12345Z",
                      "modified_on": "2014-01-01T05:20:00.12345Z",
                      "scope": {
                        "id": "fake-rule-scope",
                        "name": "Some Zone",
                        "type": "account"
                      }
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": []
                }"""
      )
  }

  def createAccessControlRuleForAccount(accountId: AccountId, ruleId: AccessControlRuleId) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "accounts" / id / "firewall" / "access_rules" / "rules" if id == accountId =>
      for {
        input <- req.decodeJson[AccessControlRule]
        created = input.copy(
          id = Option(ruleId),
          created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
          modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
          scope = Option(AccessControlRuleScope.Account(
            id = shapeless.tag[AccessControlRuleScopeIdTag][String]("fake-rule-scope"),
            name = Option(shapeless.tag[AccessControlRuleScopeNameTag][String]("Some Account"))
          ))
        )
        resp <- Ok(ResponseDTO(
          result = created,
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      } yield resp
  }

  def createAccessControlRuleForZone(zoneId: ZoneId, ruleId: AccessControlRuleId) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "zones" / id / "firewall" / "access_rules" / "rules" if id == zoneId =>
      for {
        input <- req.decodeJson[AccessControlRule]
        created = input.copy(
          id = Option(ruleId),
          created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
          modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
          scope = Option(AccessControlRuleScope.Zone(
            id = shapeless.tag[AccessControlRuleScopeIdTag][String]("fake-rule-scope"),
            name = Option(shapeless.tag[AccessControlRuleScopeNameTag][String]("Some Zone"))
          ))
        )
        resp <- Ok(ResponseDTO(
          result = created,
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      } yield resp
  }

  def updateAccessControlRule(level: Level, ruleId: AccessControlRuleId) = {
    def buildResponse(req: Request[IO], scope: AccessControlRuleScope) = for {
      input <- req.decodeJson[AccessControlRule]
      resp <- if (input.id.isEmpty)
        Ok(ResponseDTO(
          result = input.copy(
            id = Option(ruleId),
            modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
            scope = Option(scope)
          ),
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      else
        BadRequest("input ID should be empty")
    } yield resp

    HttpRoutes.of[IO] {
      case req@PATCH -> Root / "client" / "v4" / lev / id / "firewall" / "access_rules" / "rules" / rid if rid == ruleId =>
        (lev, level) match {
          case ("accounts", Level.Account(aid)) if aid == id =>
            val scope = AccessControlRuleScope.Account(
              id = shapeless.tag[AccessControlRuleScopeIdTag][String]("fake-rule-scope"),
              name = Option(shapeless.tag[AccessControlRuleScopeNameTag][String]("Some Account"))
            )
            buildResponse(req, scope)
          case ("zones", Level.Zone(zid)) if zid == id =>
            val scope = AccessControlRuleScope.Zone(
              id = shapeless.tag[AccessControlRuleScopeIdTag][String]("fake-rule-scope"),
              name = Option(shapeless.tag[AccessControlRuleScopeNameTag][String]("Some Zone"))
            )
            buildResponse(req, scope)
        }
    }
  }

  def deleteAccessControlRule(level: Level, ruleId: AccessControlRuleId) = {
    val response = Ok(
      json"""{
                 "result": {
                   "id": ${ruleId: String}
                 },
                 "success": true,
                 "errors": [],
                 "messages": []
               }""")

    HttpRoutes.of[IO] {
      case DELETE -> Root / "client" / "v4" / lev / id / "firewall" / "access_rules" / "rules" / rid if rid == ruleId =>
        (lev, level) match {
          case ("accounts", Level.Account(aid)) if aid == id => response
          case ("zones", Level.Zone(zid)) if zid == id => response
        }
    }
  }

  def deleteAccessControlThatDoesNotExist(level: Level) = {
    val response = Ok(
      json"""{
                 "result": null,
                 "success": true,
                 "errors": null,
                 "messages": null
               }""")

    HttpRoutes.of[IO] {
      case DELETE -> Root / "client" / "v4" / lev / id / "firewall" / "access_rules" / "rules" / _ if List("accounts", "zones").contains(lev) =>
        (lev, level) match {
          case ("accounts", Level.Account(aid)) if aid == id => response
          case ("zones", Level.Zone(zid)) if zid == id => response
        }
    }
  }

  def listAccounts(pages: Map[Int, String]) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" :? OptionalPageQueryParamMatcher(pageQuery) +& DirectionPageQueryParamMatcher(directionQuery) =>
      if (directionQuery != "asc") BadRequest()
      else {
        pages.get(pageQuery.getOrElse(1)).fold(BadRequest()) { pageBody =>
          okWithJson(pageBody)
        }
      }
  }

  def accountById(responseBody: String, accountId: String, status: Status = Status.Ok) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / account =>
      if (account != accountId) BadRequest("Invalid account id")
      else {
        IO(Response(status).withEntity(parseJson(responseBody)))
      }
  }

  def listAccountRoles(pages: Map[Int, String], accountId: String) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / account / "roles" :? OptionalPageQueryParamMatcher(pageQuery) =>
      if (account != accountId) BadRequest()
      else {
        pages.get(pageQuery.getOrElse(1)).fold(BadRequest()) { pageBody =>
          okWithJson(pageBody)
        }
      }
  }

  def getAccountMember(responseBody: String, accountId: String, accountMemberId: String, status: Status = Status.Ok) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "accounts" / account / "members" / accountMember =>
      if (account != accountId || accountMember != accountMemberId) BadRequest()
      else {
        IO(Response(status).withEntity(parseJson(responseBody)))
      }
  }

  def addAccountMember(responseBody: String, accountId: String, status: Status = Status.Ok) = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "accounts" / account / "members" =>
      if (account != accountId) BadRequest()
      else {
        IO(Response(status).withEntity(parseJson(responseBody)))
      }
  }

  def updateAccountMember(responseBody: String, accountId: String, accountMemberId: String, status: Status = Status.Ok) = HttpRoutes.of[IO] {
    case PUT -> Root / "client" / "v4" / "accounts" / account / "members" / accountMember =>
      if (account != accountId || accountMember != accountMemberId) BadRequest()
      else {
        IO(Response(status).withEntity(parseJson(responseBody)))
      }
  }

  def removeAccountMember(responseBody: String, accountId: String, accountMemberId: String, status: Status = Status.Ok) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "accounts" / account / "members" / accountMember =>
      if (account != accountId || accountMember != accountMemberId) BadRequest()
      else {
        IO(Response(status).withEntity(parseJson(responseBody)))
      }
  }

  def setTlsLevelService(zoneId: String, expectedValue: String) = HttpRoutes.of[IO] {
    case req@PATCH -> Root / "client" / "v4" / "zones" / id / "settings" / "ssl" if id == zoneId =>
      for {
        input <- req.decodeJson[CloudflareSettingValue]
        _ <- if (input.value != expectedValue) IO.raiseError(new RuntimeException(s"Expected $expectedValue but got ${input.value}")) else IO.unit
        res <- okWithJson(
          """{
            |  "success": true,
            |  "errors": [],
            |  "messages": [],
            |  "result": {
            |    "id": "ssl",
            |    "value": "full",
            |    "editable": true,
            |    "modified_on": "2014-01-01T05:20:00.12345Z"
            |  }
            |}
          """.stripMargin)
      } yield res
  }

  def setSecurityLevelService(zoneId: String, expectedValue: String) = HttpRoutes.of[IO] {
    case req@PATCH -> Root / "client" / "v4" / "zones" / id / "settings" / "security_level" if id == zoneId =>
      for {
        input <- req.decodeJson[CloudflareSettingValue]
        _ <- if (input.value != expectedValue) IO.raiseError(new RuntimeException(s"Expected $expectedValue but got ${input.value}")) else IO.unit
        res <- okWithJson(
          """{
            |  "success": true,
            |  "errors": [],
            |  "messages": [],
            |  "result": {
            |    "id": "security_level",
            |    "value": "high",
            |    "editable": true,
            |    "modified_on": "2014-01-01T05:20:00.12345Z"
            |  }
            |}
          """.stripMargin)
      } yield res
  }

  def setWafService(zoneId: String, expectedValue: String) = HttpRoutes.of[IO] {
    case req@PATCH -> Root / "client" / "v4" / "zones" / id / "settings" / "waf" if id == zoneId =>
      for {
        input <- req.decodeJson[CloudflareSettingValue]
        _ <- if (input.value != expectedValue) IO.raiseError(new RuntimeException(s"Expected $expectedValue but got ${input.value}")) else IO.unit
        res <- okWithJson(
          """{
            |  "success": true,
            |  "errors": [],
            |  "messages": [],
            |  "result": {
            |    "id": "waf",
            |    "value": "on",
            |    "editable": true,
            |    "modified_on": "2014-01-01T05:20:00.12345Z"
            |  }
            |}
          """.stripMargin)
      } yield res
  }

  def listLogpushJobs(zoneId: String, responseBody: Json) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "logpush" / "jobs" if id == zoneId =>
      Ok(responseBody)
  }

  def createLogpushOwnership(zoneId: String, responseBody: Json) = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / id / "logpush" / "ownership" if id == zoneId =>
      Ok(responseBody)
  }

  def createLogpushJob(zoneId: String, responseBody: Json) = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / id / "logpush" / "jobs" if id == zoneId =>
      Ok(responseBody)
  }

  def listPageRules(zoneId: ZoneId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "pagerules" if id == zoneId =>
      Ok(
        json"""{
                 "result": [
                   {
                     "id": "50fdc2d542e0f6c6246963277d1dc140",
                     "targets": [
                       {
                         "target": "url",
                         "constraint": {
                           "operator": "matches",
                           "value": "http://hydragents.xyz/"
                         }
                       }
                     ],
                     "actions": [
                       {
                         "id": "forwarding_url",
                         "value": {
                           "url": "http://hydragents.xyz/home",
                           "status_code": 301
                         }
                       }
                     ],
                     "priority": 2,
                     "status": "disabled",
                     "created_on": "2019-01-18T21:33:59.000000Z",
                     "modified_on": "2019-01-23T01:03:53.000000Z"
                   },
                   {
                     "id": "b7cc3152e872cf6e02384706fbabcc7f",
                     "targets": [
                       {
                         "target": "url",
                         "constraint": {
                           "operator": "matches",
                           "value": "http://*.hydragents.xyz/*"
                         }
                       }
                     ],
                     "actions": [
                       {
                         "id": "always_use_https"
                       }
                     ],
                     "priority": 1,
                     "status": "disabled",
                     "created_on": "2017-03-27T17:28:36.000000Z",
                     "modified_on": "2017-03-27T18:33:11.000000Z"
                   }
                 ],
                 "success": true,
                 "errors": [],
                 "messages": []
               }"""
      )
  }

  def getPageRuleById(zoneId: ZoneId, pageRuleId: PageRuleId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / zid / "pagerules" / pid if zid == zoneId && pid == pageRuleId =>
      Ok(
        json"""{
                 "result": {
                   "id": "50fdc2d542e0f6c6246963277d1dc140",
                   "targets": [
                     {
                       "target": "url",
                       "constraint": {
                         "operator": "matches",
                         "value": "http://hydragents.xyz/"
                       }
                     }
                   ],
                   "actions": [
                     {
                       "id": "forwarding_url",
                       "value": {
                         "url": "http://hydragents.xyz/home",
                         "status_code": 301
                       }
                     }
                   ],
                   "priority": 2,
                   "status": "disabled",
                   "created_on": "2019-01-18T21:33:59.000000Z",
                   "modified_on": "2019-01-23T01:03:53.000000Z"
                 },
                 "success": true,
                 "errors": [],
                 "messages": []
               }"""
      )
  }

  def createPageRule(zoneId: ZoneId, pageRuleId: PageRuleId) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "zones" / zid / "pagerules" if zid == zoneId =>
      implicitly[Encoder[PageRule]](importedEncoder[PageRule])
      implicitly[Encoder[ResponseDTO[PageRule]]]
      for {
        input <- req.decodeJson[PageRule]
        created = input.copy(
          id = Option(pageRuleId),
          created_on = Option("1983-09-10T21:33:59.000000Z").map(Instant.parse),
          modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse),
        )
        resp <- Ok(ResponseDTO(
          result = created,
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      } yield resp
  }

  val createPageRuleFails = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / _ / "pagerules" =>
      Ok(
        json"""{
                 "success": false,
                 "errors": [
                   {
                     "code": 1004,
                     "message": "Page Rule validation failed: See messages for details."
                   }
                 ],
                 "messages": [
                   {
                     "code": 1,
                     "message": ".distinctTargetUrl: Your zone already has an existing page rule with that URL. If you are modifying only page rule settings use the Edit Page Rule option instead",
                     "type": null
                   }
                 ],
                 "result": null
               }
               """)
  }

  def updatePageRule(zoneId: ZoneId, pageRuleId: PageRuleId) = HttpRoutes.of[IO] {
    case req@PUT -> Root / "client" / "v4" / "zones" / zid / "pagerules" / pid if zid == zoneId && pid == pageRuleId =>
      for {
        input <- req.decodeJson[PageRule]
        resp <- if (input.id.isEmpty)
          Ok(ResponseDTO(
            result = input.copy(id = Option(pageRuleId), modified_on = Option("2019-01-24T11:09:11.000000Z").map(Instant.parse)),
            success = true,
            errors = None,
            messages = None,
          ).asJson)
        else
          BadRequest("input ID should be empty")
      } yield resp
  }

  def deletePageRule(zoneId: ZoneId, pageRuleId: PageRuleId) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "pagerules" / pid if zid == zoneId && pid == pageRuleId =>
      Ok(
        json"""{
                 "result": {
                   "id": ${pageRuleId: String}
                 },
                 "success": true,
                 "errors": [],
                 "messages": []
               }""")
  }

  def deletePageRuleThatDoesNotExist(zoneId: ZoneId, validId: Boolean) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "pagerules" / _ if zid == zoneId =>
      if (validId)
        Ok(
          json"""{
                   "success": false,
                   "errors": [
                     {
                       "code": 1002,
                       "message": "Invalid Page Rule identifier"
                     }
                   ],
                   "messages": [],
                   "result": null
                 }""")
      else
        Ok(
          json"""{
                   "success": false,
                   "errors": [
                     {
                       "code": 7003,
                       "message": "Could not route to /zones/90940840480ba654a3a5ddcdc5d741f9/pagerules/asdf, perhaps your object identifier is invalid?"
                     },
                     {
                       "code": 7000,
                       "message": "No route for that URI"
                     }
                   ],
                   "messages": [],
                   "result": null
                 }""")

  }

  def listFirewallRules(zoneId: ZoneId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / id / "firewall" / "rules" if id == zoneId =>
      Ok(
        json"""{
                  "result": [
                    {
                      "id": "ccbfa4a0b26b4ffa8a006e8b11557397",
                      "paused": false,
                      "description": "rule1",
                      "action": "log",
                      "priority": 1,
                      "filter": {
                        "id": "d5266c8daa9443e081e5207f64763836",
                        "expression": "(cf.bot_management.verified_bot)",
                        "paused": false
                      },
                      "created_on": "2019-12-14T01:38:21Z",
                      "modified_on": "2019-12-14T01:38:21Z"
                    },
                    {
                      "id": "c41d348b8ff64bc8a7f4f8b58c986c4c",
                      "paused": false,
                      "description": "rule2",
                      "action": "challenge",
                      "priority": 2,
                      "filter": {
                        "id": "308d8c703fa14939b563c84db4320fee",
                        "expression": "(ip.src ne 0.0.0.0)",
                        "paused": false
                      },
                      "created_on": "2019-12-14T01:39:06Z",
                      "modified_on": "2019-12-14T01:39:06Z"
                    }
                  ],
                  "success": true,
                  "errors": [],
                  "messages": [],
                  "result_info": {
                    "page": 1,
                    "per_page": 25,
                    "count": 2,
                    "total_count": 2,
                    "total_pages": 1
                  }
                }"""
      )
  }

  def getFirewallRuleById(zoneId: ZoneId, firewallRuleId: FirewallRuleId) = HttpRoutes.of[IO] {
    case GET -> Root / "client" / "v4" / "zones" / zid / "firewall" / "rules"/ fid if zid == zoneId && fid == firewallRuleId =>
      Ok(
        json"""{
                  "result": {
                    "id": "ccbfa4a0b26b4ffa8a006e8b11557397",
                    "paused": false,
                    "description": "rule1",
                    "action": "log",
                    "priority": 1,
                    "filter": {
                      "id": "d5266c8daa9443e081e5207f64763836",
                      "expression": "(cf.bot_management.verified_bot)",
                      "paused": false
                    },
                    "created_on": "2019-12-14T01:38:21Z",
                    "modified_on": "2019-12-14T01:38:21Z"
                  },
                  "success": true,
                  "errors": [],
                  "messages": []
          }"""
      )
  }

  def createFirewallRule(zoneId: ZoneId, firewallRuleId: FirewallRuleId) = HttpRoutes.of[IO] {
    case req@POST -> Root / "client" / "v4" / "zones" / zid / "firewall" / "rules" if zid == zoneId =>
      for {
        input <- req.decodeJson[List[FirewallRule]](Decoder.decodeList[FirewallRule])
        created = input.map(_.copy(
          id = Option(firewallRuleId),
          created_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
          modified_on = Option("2019-12-14T01:38:21Z").map(Instant.parse),
        ))
        resp <- Ok(ResponseDTO(
          result = created,
          success = true,
          errors = None,
          messages = None,
        ).asJson)
      } yield resp
  }

  val createFirewallRuleFails = HttpRoutes.of[IO] {
    case POST -> Root / "client" / "v4" / "zones" / _ / "firewall" / "rules" =>
      Ok(
        json"""{
                  "result": null,
                  "success": false,
                  "errors": [
                    {
                      "message": "products is only valid for the 'bypass' action",
                      "source": {
                        "pointer": "/0"
                      }
                    }
                  ],
                  "messages": null
                }""")
  }

  def updateFirewallRule(zoneId: ZoneId, firewallRuleId: FirewallRuleId) = HttpRoutes.of[IO] {
    case req@PUT -> Root / "client" / "v4" / "zones" / zid / "firewall" / "rules" / fid if zid == zoneId && fid == firewallRuleId =>
      for {
        input <- req.decodeJson[FirewallRule]
        resp <- if (input.id.isEmpty)
          Ok(ResponseDTO(
            result = input.copy(
              id = Option(firewallRuleId),
              modified_on = Option("2019-12-14T01:39:58Z").map(Instant.parse)
            ),
            success = true,
            errors = None,
            messages = None,
          ).asJson)
        else
          BadRequest("input ID should be empty")
      } yield resp
  }

  def deleteFirewallRule(zoneId: ZoneId, firewallRuleId: FirewallRuleId) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "firewall" / "rules" / fid if zid == zoneId && fid == firewallRuleId =>
      Ok(
        json"""{
                 "result": {
                   "id": ${firewallRuleId: String}
                 },
                 "success": true,
                 "errors": [],
                 "messages": []
               }""")
  }

  def deleteFirewallRuleThatDoesNotExist(zoneId: ZoneId, firewallRuleId: FirewallRuleId, validId: Boolean) = HttpRoutes.of[IO] {
    case DELETE -> Root / "client" / "v4" / "zones" / zid / "firewall" / "rules" / _ if zid == zoneId =>
      if (validId)
        Ok(
          json"""{
                    "result": {
                      "id": ${firewallRuleId: String}
                    },
                    "success": true,
                    "errors": [],
                    "messages": []
                  }""")
      else
        Ok(
          json"""{
                   "success": false,
                   "errors": [
                     {
                       "code": 7003,
                       "message": "Could not route to /zones/90940840480ba654a3a5ddcdc5d741f9/firewall/rules/asdf, perhaps your object identifier is invalid?"
                     },
                     {
                       "code": 7000,
                       "message": "No route for that URI"
                     }
                   ],
                   "messages": [],
                   "result": null
                 }""")

  }

  private def okWithJson(responseBody: String) = {
    Ok(parseJson(responseBody))
  }

  private def parseJson(responseBody: String): Json = {
    parse(responseBody) match {
      case Left(ex) => throw ex
      case Right(x) => x
    }
  }

  def cloudflareApi(service: HttpRoutes[IO]) = Kleisli[OptionT[IO, *], Request[IO], Response[IO]] { req =>
    req.headers.get(CaseInsensitiveString("X-Auth-Email")) match {
      case Some(Header(_, email)) if email == authorization.email =>
        req.headers.get(CaseInsensitiveString("X-Auth-Key")) match {
          case Some(Header(_, key)) if key == authorization.key =>
            VirtualHost(exact(service, "api.cloudflare.com")).run(req)
          case _ => OptionT.liftF(Forbidden())
        }
      case _ => OptionT.liftF(Forbidden())
    }
  }

  def client(service: HttpRoutes[IO]) = Client.fromHttpApp(cloudflareApi(service).orNotFound)
}
