/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customs.imports.config

import com.typesafe.config.{Config, ConfigFactory}
import java.util.UUID
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.test.UnitSpec

class AppConfigSpec extends UnitSpec with MockitoSugar {
  private val validAppConfig: Config = ConfigFactory.parseString(
    """
      |microservice.services.customs-declarations.host=remotedec-api
      |microservice.services.customs-declarations.port=6000
      |microservice.services.customs-declarations.api-version=1.0
      |microservice.services.customs-declarations.submit-uri=/declarations
    """.stripMargin)

  private val emptyAppConfig: Config = ConfigFactory.parseString("")

  private val validServicesConfiguration = Configuration(validAppConfig)
  private val emptyServicesConfiguration = Configuration(emptyAppConfig)


  private def customsConfigService(conf: Configuration) =
    new AppConfig(runModeConfiguration = conf,  mock[Environment]) {
      override val mode: Mode.Value = play.api.Mode.Test
    }

  "AppConfig" should {
    "return config as object model when configuration is valid" in {
      val configService: AppConfig = customsConfigService(validServicesConfiguration)

      configService.customsDeclarationsApiVersion shouldBe "1.0"
      configService.submitImportDeclarationUri shouldBe "/declarations"
      configService.customsDeclarationsBaseUrl shouldBe "http://remotedec-api:6000"
    }

    "throw an exception when mandatory configuration is invalid" in {
      val configService: AppConfig = customsConfigService(emptyServicesConfiguration)

      val caught1: RuntimeException = intercept[RuntimeException](configService.customsDeclarationsBaseUrl)
      caught1.getMessage shouldBe "Could not find config customs-declarations.host"

      val caught3: RuntimeException = intercept[RuntimeException](configService.customsDeclarationsApiVersion)
      caught3.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.api-version'"

      val caught4: RuntimeException = intercept[RuntimeException](configService.submitImportDeclarationUri)
      caught4.getMessage shouldBe "Could not find config key 'microservice.services.customs-declarations.submit-uri'"
    }

    "developerHubClientId" should {
      val appName = "customs-declare-imports"
      val clientId = UUID.randomUUID.toString

      "return the configured value when explicitly set" in {
        val configService = customsConfigService(Configuration("appName" -> appName, "microservice.services.customs-declarations.client-id" -> clientId))

        configService.developerHubClientId shouldBe clientId
      }

      "return the app name when no explicit config exists" in {
        val configService = customsConfigService(Configuration("appName" -> appName))

        configService.developerHubClientId shouldBe appName
      }
    }
  }

}
