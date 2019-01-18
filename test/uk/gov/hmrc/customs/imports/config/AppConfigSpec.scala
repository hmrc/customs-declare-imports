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

import uk.gov.hmrc.customs.imports.base.CustomsImportsBaseSpec

class AppConfigSpec extends CustomsImportsBaseSpec {
  val config: AppConfig = app.injector.instanceOf[AppConfig]

  "The config" should {
    "have auth url" in {
      config.authUrl must be("http://localhost:8500")
    }

    "have login url" in {
      config.loginUrl must be("http://localhost:9949/auth-login-stub/gg-sign-in")
    }

    "have cancelImportDeclarationUri" in {
      config.cancelImportDeclarationUri must be("/cancellation-requests")
    }

    "have customsDeclarationsApiVersion" in {
      config.customsDeclarationsApiVersion must be("1.0")
    }

    "have customsDeclarationsEndpoint" in {
      config.customsDeclarationsEndpoint must be("http://localhost:6790")
    }

  }
}
