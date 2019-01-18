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

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import play.api.Configuration
import play.api.http.{ContentTypes, HeaderNames, HttpVerbs, Status}
import play.api.libs.ws.WSClient
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.customs.imports.base.{CustomsImportsBaseSpec, ImportsTestData}
import uk.gov.hmrc.customs.imports.models.{SignedInUser, Submission}
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.wco.dec.MetaData
import uk.gov.hmrc.customs.imports.controllers.CustomsHeaderNames._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.customs.imports.utils.HttpRequest


class CustomsDeclarationsConnectorSpec extends CustomsImportsBaseSpec with ImportsTestData {

  // any required implicits (++ those in CustomsSpec)

  implicit val mc: ReactiveMongoComponent = component[ReactiveMongoComponent]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  // some basic fixtures and helpers

  val submitUrl: String = s"${appConfig.customsDeclarationsEndpoint}${appConfig.submitImportDeclarationUri}"

  val acceptContentType: String = s"application/vnd.hmrc.${appConfig.customsDeclarationsApiVersion}+xml"

  val aRandomSubmitDeclaration: MetaData = randomSubmitDeclaration

  def acceptedResponse(conversationId: String) = HttpResponse(
    responseStatus = Status.ACCEPTED,
    responseHeaders = Map(XConversationIdName -> Seq(conversationId))
  )

  def otherResponse(status: Int) = HttpResponse(responseStatus = status)

  def submitRequest(submissionPayload: String, headers: Map[String, String]): HttpRequest = HttpRequest(submitUrl, submissionPayload, headers)

  def expectingAcceptedResponse(request: HttpRequest): Either[Exception, HttpExpectation] = Right(HttpExpectation(
    request,
    acceptedResponse(randomConversationId)
  ))

  def expectingOtherResponse(request: HttpRequest, status: Int, headers: Map[String, String]): Either[Exception, HttpExpectation] = Right(HttpExpectation(
    request,
    otherResponse(status)
  ))

  def expectingFailure(ex: Exception): Either[Exception, HttpExpectation] = Left(ex)

  // the actual test scenarios

  "submit import declaration" should {

    "specify X-Client-ID in request headers" in simpleAcceptedSubmissionScenario(declarantEoriValue, declarantLrnValue, aRandomSubmitDeclaration.toXml) { (_, http, _, _, _) =>
      http.requests.head.headers("X-Client-ID") must be(appConfig.developerHubClientId)
    }

    "specify correct Accept value in request headers" in simpleAcceptedSubmissionScenario(declarantEoriValue, declarantLrnValue, aRandomSubmitDeclaration.toXml) { (_, http, _, _, _) =>
      http.requests.head.headers(HeaderNames.ACCEPT) must be(acceptContentType)
    }

    "specify Content-Type as XML in request headers" in simpleAcceptedSubmissionScenario(declarantEoriValue, declarantLrnValue, aRandomSubmitDeclaration.toXml) { (_, http, _, _, _) =>
      http.requests.head.headers(HeaderNames.CONTENT_TYPE) must be(ContentTypes.XML)
    }

    "send metadata as XML in request body" in simpleAcceptedSubmissionScenario(declarantEoriValue, declarantLrnValue, aRandomSubmitDeclaration.toXml) { (_, http, _, _, _) =>
      http.requests.head.body must be(aRandomSubmitDeclaration.toXml.mkString)
    }

//    "save successfully submitted declaration" in simpleAcceptedSubmissionScenario(aRandomSubmitDeclaration) { (_, _, expectation, repo, _) =>
//      val saved = repo.findAll().futureValue.head
//      saved.lrn must be(aRandomSubmitDeclaration.declaration.value.functionalReferenceId)
//      saved.eori must be(user.eori)
//      saved.conversationId must be(expectation.resp.header("X-Conversation-ID").get)
//    }

    "throw gateway timeout exception when request times out" in {
      val ex = new TimeoutException("API is not responding")
      withHttpClient(expectingFailure(ex)) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            connector.
              submitImportDeclaration(declarantEoriValue,  aRandomSubmitDeclaration.toXml).
              failed.futureValue.
              asInstanceOf[GatewayTimeoutException].
              message must be(http.gatewayTimeoutMessage(HttpVerbs.POST, submitUrl, ex))
          }
        }
      }
    }

    "throw bad gateway exception when request cannot connect" in {
      val ex = new ConnectException("API is down")
      withHttpClient(expectingFailure(ex)) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            connector.
              submitImportDeclaration(declarantEoriValue,  aRandomSubmitDeclaration.toXml).
              failed.futureValue.
              asInstanceOf[BadGatewayException].
              message must be(http.badGatewayMessage(HttpVerbs.POST, submitUrl, ex))
          }
        }
      }
    }

    "throw upstream 5xx exception when API responds with internal server error" in {
      withHttpClient(expectingOtherResponse(submitRequest(aRandomSubmitDeclaration.toXml, ValidAPIResponseHeaders), Status.INTERNAL_SERVER_ERROR, ValidHeaders)) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            val ex = connector.submitImportDeclaration(declarantEoriValue, aRandomSubmitDeclaration.toXml).failed.futureValue.asInstanceOf[Upstream5xxResponse]
            ex.upstreamResponseCode must be(Status.INTERNAL_SERVER_ERROR)
            ex.reportAs must be(Status.INTERNAL_SERVER_ERROR)
          }
        }
      }
    }

    "throw upstream 4xx exception when API responds with bad request" in {
      withHttpClient(expectingOtherResponse(submitRequest(aRandomSubmitDeclaration.toXml, ValidAPIResponseHeaders), Status.BAD_REQUEST, ValidHeaders)) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            val ex = connector.submitImportDeclaration(declarantEoriValue, aRandomSubmitDeclaration.toXml).failed.futureValue.asInstanceOf[Upstream4xxResponse]
            ex.upstreamResponseCode must be(Status.BAD_REQUEST)
            ex.reportAs must be(Status.INTERNAL_SERVER_ERROR)
          }
        }
      }
    }

    "throw upstream 4xx exception when API responds with unauthhorised" in {
      withHttpClient(expectingOtherResponse(submitRequest(aRandomSubmitDeclaration.toXml, ValidAPIResponseHeaders), Status.UNAUTHORIZED, ValidHeaders)) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            val ex = connector.submitImportDeclaration(declarantEoriValue,  aRandomSubmitDeclaration.toXml).failed.futureValue.asInstanceOf[Upstream4xxResponse]
            ex.upstreamResponseCode must be(Status.UNAUTHORIZED)
            ex.reportAs must be(Status.INTERNAL_SERVER_ERROR)
          }
        }
      }
    }

  }



  // the test scenario builders

  def simpleAcceptedSubmissionScenario(eori: String, lrn: String, xmlPayload : String)
                                      (test: (Map[String, String], MockHttpClient, HttpExpectation, SubmissionRepository, CustomsDeclarationsConnector) => Unit): Unit = {
      val expectation = expectingAcceptedResponse(submitRequest(xmlPayload, ValidAPIResponseHeaders))
      withHttpClient(expectation) { http =>
        withSubmissionRepository() { repo =>
          withCustomsDeclarationsConnector(http, repo) { connector =>
            whenReady(connector.submitImportDeclaration(eori, xmlPayload)) { _ =>
              test(ValidAPIResponseHeaders, http, expectation.right.get, repo, connector)
            }
          }
        }
      }
  }


  def withHttpClient(throwOrRespond: Either[Exception, HttpExpectation])
                    (test: MockHttpClient => Unit): Unit = {
    test(new MockHttpClient(throwOrRespond, component[Configuration], component[AuditConnector], component[WSClient], component[ActorSystem]))
  }

  def withSubmissionRepository()(test: SubmissionRepository => Unit): Unit = test(new SubmissionRepository() {

    val inserted: mutable.Buffer[Submission] = mutable.Buffer.empty

    // abuse findAll function for testing purposes :)
    override def findAll(readPreference: ReadPreference = ReadPreference.primaryPreferred)
                        (implicit ec: ExecutionContext): Future[List[Submission]] = Future.successful(inserted.toList)

    override def findByEori(eori: String): Future[Seq[Submission]] = throw new IllegalArgumentException("Unexpected call")

    override def getByConversationId(conversationId: String): Future[Option[Submission]] = throw new IllegalArgumentException("Unexpected call")

    override def getByEoriAndMrn(eori: String, mrn: String): Future[Option[Submission]] = throw new IllegalArgumentException("Unexpected call")

    override def insert(entity: Submission)(implicit ec: ExecutionContext): Future[WriteResult] = {
      inserted += entity
      Future.successful(DefaultWriteResult(ok = true, 1, Seq.empty, None, None, None))
    }

    override def isInsertion(newRecordId: BSONObjectID, oldRecord: Submission): Boolean = throw new IllegalArgumentException("Unexpected call")

  })

  def withCustomsDeclarationsConnector(httpClient: HttpClient, submissionRepository: SubmissionRepository)
                                      (test: CustomsDeclarationsConnector => Unit): Unit = {
    test(new CustomsDeclarationsConnector(appConfig, httpClient, submissionRepository))
  }

}

case class HttpExpectation(req: uk.gov.hmrc.customs.imports.utils.HttpRequest, resp: HttpResponse)



class MockHttpClient(throwOrRespond: Either[Exception, HttpExpectation], config: Configuration, auditConnector: AuditConnector, wsClient: WSClient, actorSystem: ActorSystem) extends DefaultHttpClient(config, auditConnector, wsClient, actorSystem = actorSystem) {

  val requests: mutable.Buffer[uk.gov.hmrc.customs.imports.utils.HttpRequest] = mutable.Buffer.empty


  override def doPostString(url: String, body: String, headers: Seq[(String, String)])
                           (implicit hc: HeaderCarrier): Future[HttpResponse] = {
    requests += uk.gov.hmrc.customs.imports.utils.HttpRequest(url, body, headers.toMap)
    throwOrRespond.fold(
      ex => Future.failed(ex),
      respond =>
        if (url == respond.req.url && body == respond.req.body) Future.successful(respond.resp)
        else super.doPostString(url, body, headers)
    )
  }

}
