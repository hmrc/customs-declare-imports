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
      configService.customsDeclarationsHostName shouldBe "remotedec-api"
      configService.customsDeclarationsPort shouldBe "6000"
    }


    "throw an exception when configuration is invalid" in {
      val configService: AppConfig = customsConfigService(emptyServicesConfiguration)

      val caught1: IllegalStateException = intercept[IllegalStateException](configService.customsDeclarationsHostName)
      caught1.getMessage shouldBe "Missing configuration for Customs Declarations HostName"

      val caught2: IllegalStateException = intercept[IllegalStateException](configService.customsDeclarationsPort)
      caught2.getMessage shouldBe "Missing configuration for Customs Declarations Port"

      val caught3: IllegalStateException = intercept[IllegalStateException](configService.customsDeclarationsApiVersion)
      caught3.getMessage shouldBe "Missing configuration for Customs Declarations API version"

      val caught4: IllegalStateException = intercept[IllegalStateException](configService.submitImportDeclarationUri)
      caught4.getMessage shouldBe "Missing configuration for Customs Declarations submission URI"
    }
  }

}
