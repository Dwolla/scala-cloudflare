package com.dwolla.cloudflare.domain.model

import io.circe.*
import io.circe.generic.semiauto
import com.dwolla.cloudflare.CloudflareNewtype
import org.http4s.{QueryParam, QueryParamEncoder, QueryParameterKey, QueryParameterValue}

package object wafrulepackages {
  type WafRulePackageName = WafRulePackageName.Type
  object WafRulePackageName extends CloudflareNewtype[String] {
    implicit val queryParam: QueryParam[WafRulePackageName] = new QueryParam[WafRulePackageName] {
      override def key: QueryParameterKey = QueryParameterKey("name")
    }
    implicit val queryParamEncoder: QueryParamEncoder[WafRulePackageName] = value => QueryParameterValue(value.value)
    implicit val jsonEncoder: Encoder[WafRulePackageName] = Encoder[String].contramap(_.value)
    implicit val jsonDecoder: Decoder[WafRulePackageName] = Decoder[String].map(WafRulePackageName(_))
  }
}

package wafrulepackages {
  trait WafRulePackageNameTag

  case class WafRulePackage(id: WafRulePackageId,
                            name: WafRulePackageName,
                            description: Option[String] = None,
                            zone_id: ZoneId,
                            detection_mode: DetectionMode,
                            sensitivity: Option[Sensitivity] = None,
                            action_mode: Option[ActionMode] = None)

  sealed trait DetectionMode
  object DetectionMode {
    case object Anomaly extends DetectionMode
    case object Traditional extends DetectionMode

    implicit val detectionModeEncoder: Encoder[DetectionMode] = Encoder[String].contramap {
      case DetectionMode.Anomaly => "anomaly"
      case DetectionMode.Traditional => "traditional"
    }

    implicit val detectionModeDecoder: Decoder[DetectionMode] = Decoder[String].map {
      case "anomaly" => DetectionMode.Anomaly
      case "traditional" => DetectionMode.Traditional
    }
  }

  sealed trait Status
  object Status {
    case object Active extends Status

    implicit val statusEncoder: Encoder[Status] = Encoder[String].contramap {
      case Status.Active => "active"
    }

    implicit val statusDecoder: Decoder[Status] = Decoder[String].emap {
      case "active" => Right(Status.Active)
      case other => Left(s""""$other" is not a valid status""")
    }
  }

  sealed trait Sensitivity
  object Sensitivity {
    case object High extends Sensitivity
    case object Medium extends Sensitivity
    case object Low extends Sensitivity
    case object Off extends Sensitivity

    implicit val sensitivityEncoder: Encoder[Sensitivity] = Encoder[String].contramap {
      case Sensitivity.High => "high"
      case Sensitivity.Medium => "medium"
      case Sensitivity.Low => "low"
      case Sensitivity.Off => "off"
    }

    implicit val sensitivityDecoder: Decoder[Sensitivity] = Decoder[String].map {
      case "high" => Sensitivity.High
      case "medium" => Sensitivity.Medium
      case "low" => Sensitivity.Low
      case "off" => Sensitivity.Off
    }
  }

  sealed trait ActionMode
  object ActionMode {
    case object Simulate extends ActionMode
    case object Block extends ActionMode
    case object Challenge extends ActionMode

    implicit val actionModeEncoder: Encoder[ActionMode] = Encoder[String].contramap {
      case ActionMode.Simulate => "simulate"
      case ActionMode.Block => "block"
      case ActionMode.Challenge => "challenge"
    }

    implicit val actionModeDecoder: Decoder[ActionMode] = Decoder[String].map {
      case "simulate" => ActionMode.Simulate
      case "block" => ActionMode.Block
      case "challenge" => ActionMode.Challenge
    }
  }

  object WafRulePackage {
    implicit val wafRulePackageCodec: Codec[WafRulePackage] = semiauto.deriveCodec
  }
}
