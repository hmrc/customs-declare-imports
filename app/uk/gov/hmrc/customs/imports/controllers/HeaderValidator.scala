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
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames.XLrnHeaderName
import uk.gov.hmrc.customs.imports.models.{LocalReferenceNumber, ValidatedHeadersRequest}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class HeaderValidator {

  def extractLrnHeader(headers: Seq[(String, String)]): Option[String] = {
    headers.toMap[String, String].get(XLrnHeaderName)
  }

  def validateAndExtractHeaders(implicit hc: HeaderCarrier, request: Request[AnyContent]):
  Either[ErrorResponse, ValidatedHeadersRequest[AnyContent]] = {
    val result = for{
      lrn <- extractLrnHeader(hc.headers)
    } yield ValidatedHeadersRequest(LocalReferenceNumber(lrn), request)
    result match {
      case Some(vhr) =>
        Right(vhr)
      case _ =>
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }
}


