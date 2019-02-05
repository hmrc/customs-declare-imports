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
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.services.ImportService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class NotificationController @Inject()(
                                      appConfig: AppConfig,
                                      headerValidator: HeaderValidator,
                                      importService: ImportService
) extends BaseController {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Left(Ok)
  })
  def handleNotification(): Action[AnyContent] = Action.async(bodyParser = xmlOrEmptyBody) { implicit request =>
    request.body.asXml match {
      case Some(xml) => processNotification(request.headers.toSimpleMap, xml)
      case None => {
        Logger.error("no xml received")
        Future.successful(Ok)
      }
    }

  }

  private def processNotification(headers: Map[String, String], xml: NodeSeq): Future[Status] = {
    headerValidator.extractConversatioIdHeader(headers).fold(Future.successful(Ok)) {
     conversationId => {
       importService.handleNotificationReceived(conversationId, xml).map({ result =>
          if (result) {
            Ok
          } else {
            Logger.info("unable to processNotification")
            Ok
          }
        })
     }
   }
  }


}
