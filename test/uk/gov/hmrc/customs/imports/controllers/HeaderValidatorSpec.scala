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

package uk.gov.hmrc.customs.imports.controllers


import uk.gov.hmrc.customs.imports.base.ImportsTestData
import uk.gov.hmrc.play.test.UnitSpec


class HeaderValidatorSpec extends UnitSpec  with ImportsTestData{

  trait SetUp {
    val validator = new HeaderValidator
  }

  "HeaderValidator" should {

    "return Eori from header when extractEori is called and eori is present" in new SetUp {
      val extractedEori: Option[String] = validator.extractEoriHeader(ValidHeaders)
      extractedEori shouldBe Some(declarantEoriValue)
    }

    "return None from header when extractEori is called header not present" in new SetUp {
      val extractedEori: Option[String] = validator.extractEoriHeader(Map.empty)
      extractedEori shouldBe None
    }

    "return LRN from header when extractLRN is called and LRN is present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(ValidHeaders)
      extractedLrn shouldBe Some(declarantLrnValue)
    }

    "return None from header when extractLrnHeader is called header not present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(Map.empty)
      extractedLrn shouldBe None
    }
  }

}
