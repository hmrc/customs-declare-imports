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
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.xml.NodeSeq
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SubmissionController @Inject()(
                                      appConfig: AppConfig,
                                      authConnector: AuthConnector,
                                      headerValidator: HeaderValidator,
                                      importService: ImportService
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

  def processRequest()(implicit request: Request[AnyContent], hc: HeaderCarrier, headers: Map[String, String]): Future[Result] = {
   headerValidator.validateAndExtractHeaders match {
     case Right(vhr) =>
       authoriseWithEori(vhr).flatMap {
         case Right(ahr) => {
             request.body.asXml match {
               case Some(xml) =>
                 handleDeclarationSubmit(ahr.eori.value, ahr.localReferenceNumber.value, xml).recoverWith {
                   case e: Exception =>
                     Logger.error(s"problem calling declaration api ${e.getMessage}")
                     Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
                 }
               case None =>
                 Logger.error("body is not xml")
                 Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
             }
         }
         case Left(error) => {
           Logger.error("Problems with Authorisation")
           Future.successful(error.XmlResult)
         }
       }
     case Left(error) =>
         Logger.error("Invalid Headers found")
         Future.successful(error.XmlResult)
     }
   }


  def handleDeclarationSubmit(eori: String, lrn: String, xml: NodeSeq)(implicit hc: HeaderCarrier): Future[Result] = {
     importService.handleDeclarationSubmit(eori, lrn, xml).map(res => {
       if(res){ Accepted
       } else{ ErrorResponse.ErrorInternalServerError.XmlResult }
     })
  }


}
