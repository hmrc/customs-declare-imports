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
class ImportService @Inject()(submissionRepository: SubmissionRepository, customsDeclarationsConnector: CustomsDeclarationsConnector)
                             (implicit hc: HeaderCarrier) {
  def handleDeclarationSubmit(eori: String, localReferenceNumber: String, xml: NodeSeq): Future[Result] = {
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
