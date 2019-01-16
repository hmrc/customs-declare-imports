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
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.models._
import uk.gov.hmrc.customs.imports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(
  appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  authConnector: AuthConnector,
  notificationsRepository: NotificationsRepository
) extends ImportController(authConnector) {

  def saveSubmissionResponse(): Action[SubmissionResponse] =
    Action.async(parse.json[SubmissionResponse]) { implicit request =>
      authorizedWithEnrolment[SubmissionResponse](_ => processRequest)
    }


  private def processRequest()(implicit request: Request[SubmissionResponse], hc: HeaderCarrier): Future[Result] = {
    val body = request.body
    submissionRepository
      .save(Submission(body.eori, body.conversationId, body.ducr, body.lrn, body.mrn))
      .map(
        res =>
          if (res) {
            Logger.debug("submission data saved to DB")
            Ok(Json.toJson(ExportsResponse(OK, "Submission response saved")))
          } else {
            Logger.error("error  saving submission data to DB")
            InternalServerError("failed saving submission")
        }
      )
  }


  def getSubmission(conversationId: String): Action[AnyContent] = Action.async { implicit request =>
    authorizedWithEnrolment[AnyContent] { _ =>
      submissionRepository.getByConversationId(conversationId).map { submission =>
        Ok(Json.toJson(submission))
      }
    }
  }


  def getSubmissionsByEori: Action[AnyContent] = Action.async { implicit request =>
    authorizedWithEori[AnyContent] { authorizedRequest =>
      for {
        submissions <- submissionHelper(authorizedRequest.loggedUserEori)
        notifications <- Future.sequence(submissions.map(submission => notificationHelper(submission.conversationId)))
      } yield {
        val result = submissions.zip(notifications).map((SubmissionData.buildSubmissionData _).tupled)

        Ok(Json.toJson(result))
      }
    }
  }

  private def submissionHelper(eori: String): Future[Seq[Submission]] =
    submissionRepository.findByEori(eori)

  private def notificationHelper(conversationId: String): Future[Int] =
    notificationsRepository.getByConversationId(conversationId).map {
      case Some(notification) => notification.response.length
      case None               => 0
    }
}
