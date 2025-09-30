package com.dwolla.cloudflare.domain.model

import com.dwolla.cloudflare.CloudflareNewtype
import com.dwolla.circe.*
import io.circe.*
import io.circe.generic.semiauto

package object accesscontrolrules {

  type AccessControlRuleId = AccessControlRuleId.Type
  object AccessControlRuleId extends CloudflareNewtype[String]

  type AccessControlRuleMode = AccessControlRuleMode.Type
  object AccessControlRuleMode extends CloudflareNewtype[String]

  type AccessControlRuleConfigurationTarget = AccessControlRuleConfigurationTarget.Type
  object AccessControlRuleConfigurationTarget extends CloudflareNewtype[String]

  type AccessControlRuleConfigurationValue = AccessControlRuleConfigurationValue.Type
  object AccessControlRuleConfigurationValue extends CloudflareNewtype[String]

  type AccessControlRuleScopeId = AccessControlRuleScopeId.Type
  object AccessControlRuleScopeId extends CloudflareNewtype[String]

  type AccessControlRuleScopeName = AccessControlRuleScopeName.Type
  object AccessControlRuleScopeName extends CloudflareNewtype[String]

  type AccessControlRuleScopeEmail = AccessControlRuleScopeEmail.Type
  object AccessControlRuleScopeEmail extends CloudflareNewtype[String]

  private[cloudflare] val tagAccessControlRuleId: String => AccessControlRuleId = AccessControlRuleId(_)
  private[cloudflare] val tagAccessControlRuleMode: String => AccessControlRuleMode = AccessControlRuleMode(_)
  private[cloudflare] val tagAccessControlRuleConfigurationTarget: String => AccessControlRuleConfigurationTarget = AccessControlRuleConfigurationTarget(_)
  private[cloudflare] val tagAccessControlRuleConfigurationValue: String => AccessControlRuleConfigurationValue = AccessControlRuleConfigurationValue(_)
  private[cloudflare] val tagAccessControlRuleScopeId: String => AccessControlRuleScopeId = AccessControlRuleScopeId(_)
  private[cloudflare] val tagAccessControlRuleScopeName: String => AccessControlRuleScopeName = AccessControlRuleScopeName(_)
  private[cloudflare] val tagAccessControlRuleScopeEmail: String => AccessControlRuleScopeEmail = AccessControlRuleScopeEmail(_)
}

package accesscontrolrules {
  import java.time.Instant

  case class AccessControlRule(id: Option[AccessControlRuleId] = None,
                                 notes: Option[String] = None,
                                 allowed_modes: List[String] = List(),
                                 mode: AccessControlRuleMode,
                                 configuration: AccessControlRuleConfiguration,
                                 created_on: Option[Instant] = None,
                                 modified_on: Option[Instant] = None,
                                 scope: Option[AccessControlRuleScope] = None)

  object AccessControlRule extends DurationAsSecondsCodec with NullAsEmptyListCodec  {
    implicit val accessControlRuleCodec: Codec[AccessControlRule] = semiauto.deriveCodec
  }

  case class AccessControlRuleConfiguration(target: AccessControlRuleConfigurationTarget,
                                            value: AccessControlRuleConfigurationValue)

  object AccessControlRuleConfiguration {
    implicit val accessControlRuleConfigurationCodec: Codec[AccessControlRuleConfiguration] = semiauto.deriveCodec
  }

  sealed trait AccessControlRuleScope
  object AccessControlRuleScope {
    import io.circe.generic.extras.Configuration

    case class Organization(id: AccessControlRuleScopeId) extends AccessControlRuleScope
    case class Account(id: AccessControlRuleScopeId,
                       name: Option[AccessControlRuleScopeName]) extends AccessControlRuleScope
    case class Zone(id: AccessControlRuleScopeId,
                    name: Option[AccessControlRuleScopeName]) extends AccessControlRuleScope
    case class User(id: AccessControlRuleScopeId,
                    email: Option[AccessControlRuleScopeEmail]) extends AccessControlRuleScope

    private implicit val genDevConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withSnakeCaseConstructorNames
        .withDiscriminator("type")

    implicit val accessControlRuleScopeCodec: Codec[AccessControlRuleScope] = generic.extras.semiauto.deriveConfiguredCodec
  }
}
