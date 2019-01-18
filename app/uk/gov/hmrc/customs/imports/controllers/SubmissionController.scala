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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.connectors.{CustomsDeclarationsConnector, CustomsDeclarationsResponse}
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
      Right(AnyContentAsEmpty)
  })

  def submitDeclaration():  Action[AnyContent] = Action.async(bodyParser = xmlOrEmptyBody) { implicit request =>
    implicit val h: Headers = request.headers
    processRequest
  }



  def processRequest()(implicit request: Request[AnyContent], hc: HeaderCarrier, h: Headers): Future[Result] = {
    //    TODO in sequence we need to validate the request????
    //    TODO in sequence we need to validate the headers and extract
    //   TODO change extractHeader code to do validate and extract and return
    //    (Either Right(Value), Left ErrorResponse)


      headerValidator.validateAndExtractHeaders match {
        case Right(vhr) =>
          request.body.asXml match {
               case Some(xml) =>
                 handleDeclarationSubmit(vhr.eori, vhr.lrn, xml).map {
                   resp =>  Logger.debug(s"conversationId: ${resp.conversationId}")
                     Ok
                 }.recoverWith {
                   case e: Exception =>
                     Logger.error("problem calling declaration api")
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

  def handleDeclarationSubmit(eori: String, lrn: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[CustomsDeclarationsResponse] = {
    customsDeclarationsConnector.submitImportDeclaration(eori, xml.toString())
    ////    submissionRepository
    ////      .save(Submission(body.eori, body.conversationId, body.lrn, body.mrn))
    ////      .map(
    ////        res =>
    ////          if (res) {
    ////            Logger.debug("submission data saved to DB")
    ////            Ok(Json.toJson(ImportsResponse(OK, "Submission response saved")))
    ////          } else {
    ////            Logger.error("error  saving submission data to DB")
    ////            InternalServerError("failed saving submission")
    ////        }
    ////      )
  }


}
