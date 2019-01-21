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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.customs.imports.models.{ImportsResponse, Submission}
import uk.gov.hmrc.customs.imports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
                                      appConfig: AppConfig,
                                      submissionRepository: SubmissionRepository,
                                      authConnector: AuthConnector,
                                      headerValidator: HeaderValidator,
                                      customsDeclarationsConnector: CustomsDeclarationsConnector,
                                      notificationsRepository: NotificationsRepository
) extends ImportController(authConnector) {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Left(ErrorResponse.ErrorInvalidPayload.XmlResult)
  })

  def submitDeclaration():  Action[AnyContent] = Action.async(bodyParser = xmlOrEmptyBody) { implicit request =>
    implicit val headers: Map[String, String] = request.headers.toSimpleMap
    processRequest
  }



  def processRequest()(implicit request: Request[AnyContent], hc: HeaderCarrier, h: Map[String, String]): Future[Result] = {
    //    TODO in sequence we need to validate the headers and extract

      headerValidator.validateAndExtractHeaders match {
        case Right(vhr) =>
          request.body.asXml match {
               case Some(xml) =>
                 handleDeclarationSubmit(vhr.eori, vhr.lrn, xml).recoverWith {
                   case e : Exception =>
                     Logger.error(s"problem calling declaration api ${e.getMessage}")
                     Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
                 }
               case None =>
                  Logger.error("body is not xml")
                  Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
             }
        case Left(error) =>
          Logger.error("Invalid Headers found")
          Future.successful(error.XmlResult)
      }

  }

  def handleDeclarationSubmit(eori: String, lrn: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Result] = {
    customsDeclarationsConnector.submitImportDeclaration(eori, xml.toString()).flatMap({ response =>
      Logger.debug(s"conversationId: ${response.conversationId}")
      submissionRepository
        .save(Submission(eori, response.conversationId, lrn, None))
        .map({ res =>
          if (res) {
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
