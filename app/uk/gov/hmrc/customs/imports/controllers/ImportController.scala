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
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, _}
import uk.gov.hmrc.customs.imports.models.{AuthorizedImportRequest, Eori, ValidatedHeadersRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ImportController @Inject()(override val authConnector: AuthConnector)(implicit ec: ExecutionContext)
    extends BaseController with AuthorisedFunctions {

    private def hasEnrolment(allEnrolments: Enrolments): Option[EnrolmentIdentifier] =
    allEnrolments.getEnrolment("HMRC-CUS-ORG").flatMap(_.getIdentifier("EORINumber"))


  def authoriseWithEori[A](vpr: ValidatedHeadersRequest)
                          (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Either[ErrorResponse, AuthorizedImportRequest]] = {
    authorised(Enrolment("HMRC-CUS-ORG")).retrieve(allEnrolments){ enrolments =>
      val eori = hasEnrolment(enrolments)
        if(eori.isDefined) {
          Future.successful(Right(AuthorizedImportRequest(vpr.localReferenceNumber, Eori(eori.get.value))))
        } else {
          Future.successful(Left(ErrorResponse.ErrorUnauthorized))
        }
    } recover {
      case _: InsufficientEnrolments =>
        Logger.warn(s"Unauthorised access for ${request.uri}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for imports"))
      case e: AuthorisationException =>
        Logger.warn(s"Unauthorised Exception for ${request.uri} ${e.reason}")
        Left(ErrorResponse.errorUnauthorized("Unauthorized for imports"))
      case ex: Throwable =>
        Logger.error("Internal server error is " + ex.getMessage)
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }



}
