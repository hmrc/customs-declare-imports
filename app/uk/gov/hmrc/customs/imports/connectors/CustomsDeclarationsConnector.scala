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

package uk.gov.hmrc.customs.imports.connectors

import com.google.inject.Inject
import javax.inject.Singleton
import models.{AuditConversation, AuditMetaData}
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.json.Json
import play.api.mvc.Codec
import uk.gov.hmrc.customs.imports.config.AppConfig
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.wco.dec.MetaData

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDeclarationsConnector @Inject()(appConfig: AppConfig,
                                             httpClient: HttpClient,
                                             submissionRepository: SubmissionRepository,
                                             auditConnector: AuditConnector) {

  def submitImportDeclaration(eori: String, xmlPayload: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] = {

    auditConnector.sendExplicitAudit("Imports-UI-Dec-Submission", AuditMetaData(eori, MetaData.fromXml(xmlPayload).toProperties))

    postMetaData(appConfig.submitImportDeclarationUri, xmlPayload, eori)
      .map { response =>
        auditConnector.sendExplicitAudit("Imports-UI-Dec-Success-failure", AuditConversation(eori, response.conversationId))
        response
      }

  }

  def cancelImportDeclaration(eori: String, xmlPayload: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] =
    postMetaData(appConfig.cancelImportDeclarationUri, xmlPayload, eori)

  private def postMetaData(uri: String,
                           xmlPayload: String,
                           eori: String,
                           onSuccess: CustomsDeclarationsResponse => Future[CustomsDeclarationsResponse] = onSuccess)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] =
    doPost(uri, xmlPayload, eori).flatMap(onSuccess(_))

  //noinspection ConvertExpressionToSAM
  private implicit val responseReader: HttpReads[CustomsDeclarationsResponse] = new HttpReads[CustomsDeclarationsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CustomsDeclarationsResponse = {
      Logger.debug(s"Response: ${response.status} => ${response.body}")
      response.status / 100 match {
        case 4 => throw Upstream4xxResponse(
          message = "Invalid request made to Customs Declarations API",
          upstreamResponseCode = response.status,
          reportAs = Status.INTERNAL_SERVER_ERROR,
          headers = response.allHeaders
        )
        case 5 => throw Upstream5xxResponse(
          message = "Customs Declarations API unable to service request",
          upstreamResponseCode = response.status,
          reportAs = Status.INTERNAL_SERVER_ERROR
        )
        case _ => CustomsDeclarationsResponse(
          response.header("X-Conversation-ID").getOrElse(
            throw Upstream5xxResponse(
              message = "Conversation ID missing from Customs Declaration API response",
              upstreamResponseCode = response.status,
              reportAs = Status.INTERNAL_SERVER_ERROR
            )
          )
        )
      }
    }
  }

  private def onSuccess(resp: CustomsDeclarationsResponse): Future[CustomsDeclarationsResponse] = Future.successful(resp)

  private def doPost(uri: String, body: String, eori: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] = {
    httpClient.POSTString[CustomsDeclarationsResponse](
      url = s"${appConfig.customsDeclarationsBaseUrl}$uri",
      body = body,
      headers = headers(eori)
    )(responseReader, hc, ec)
  }

  private def headers(eori: String): Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.developerHubClientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.customsDeclarationsApiVersion}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    CustomsHeaderNames.XEoriIdentifierHeaderName -> eori

  )

}

case class CustomsDeclarationsResponse(conversationId: String)
