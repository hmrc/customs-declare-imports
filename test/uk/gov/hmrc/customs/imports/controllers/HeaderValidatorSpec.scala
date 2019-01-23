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


import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.imports.base.ImportsTestData
import uk.gov.hmrc.customs.imports.models.{Eori, LocalReferenceNumber, ValidatedHeadersRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec


class HeaderValidatorSpec extends UnitSpec with MockitoSugar with ImportsTestData{

  trait SetUp {
    val validator = new HeaderValidator
  }

  "HeaderValidator" should {


    "return LRN from header when extractLRN is called and LRN is present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(ValidHeaders)
      extractedLrn shouldBe Some(declarantLrnValue)
    }

    "return None from header when extractLrnHeader is called header not present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(Map.empty)
      extractedLrn shouldBe None
    }

    "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
      implicit val h: Map[String, String] = ValidHeaders
      implicit val hc = mock[HeaderCarrier]

      val result: Either[ErrorResponse, ValidatedHeadersRequest] = validator.validateAndExtractHeaders
      result should be(Right(ValidatedHeadersRequest(LocalReferenceNumber(declarantLrnValue))))
    }

    "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
      implicit val h: Map[String, String] = Map("" -> "")
      val result: Either[ErrorResponse, ValidatedHeadersRequest] = validator.validateAndExtractHeaders
      result should be(Left(ErrorResponse.ErrorInternalServerError))
    }
  }

}
