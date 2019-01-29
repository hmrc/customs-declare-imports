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

package uk.gov.hmrc.customs.imports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.customs.imports.controllers.ErrorResponse
import uk.gov.hmrc.customs.imports.models.{ImportsResponse, Submission}
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Results._
import play.api.http.Status._
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class ImportService @Inject()(submissionRepository: SubmissionRepository, customsDeclarationsConnector: CustomsDeclarationsConnector) {
  def handleDeclarationSubmit(eori: String, localReferenceNumber: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Result] = {
    customsDeclarationsConnector.submitImportDeclaration(eori, xml.toString()).flatMap({ response =>
      Logger.debug(s"conversationId: ${response.conversationId}")
      submissionRepository
        .save(Submission(eori, localReferenceNumber, None))
        .map({ res =>
          if (res.ok) {
            Logger.debug("submission data saved to DB")
            Ok(Json.toJson(ImportsResponse(OK, "Submission response saved")))
          } else {
            Logger.error("error saving submission data to DB")
            ErrorResponse.ErrorInternalServerError.XmlResult
          }
        })
    })
  }


}
