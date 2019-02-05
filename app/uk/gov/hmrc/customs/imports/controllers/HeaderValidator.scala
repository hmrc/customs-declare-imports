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

import javax.inject.Singleton
import uk.gov.hmrc.customs.imports.models.{LocalReferenceNumber, ValidatedHeadersSubmissionRequest}


@Singleton
class HeaderValidator {

  def extractLrnHeader(headers: Map[String, String]): Option[String] = {
    headers.get(CustomsHeaderNames.XLrnHeaderName)
  }

  def extractConversatioIdHeader(headers: Map[String, String]): Option[String] = {
    headers.get(CustomsHeaderNames.XConversationIdName)
  }


  def validateAndExtractSubmissionHeaders(implicit headers: Map[String, String]):
  Either[ErrorResponse, ValidatedHeadersSubmissionRequest] = {
    val result = for{
      lrn <- extractLrnHeader(headers)
    } yield ValidatedHeadersSubmissionRequest(LocalReferenceNumber(lrn))
    result match {
      case Some(vhr) =>
        Right(vhr)
      case _ =>
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }
}


