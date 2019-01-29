package uk.gov.hmrc.customs.imports.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.imports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.customs.imports.models.{Eori, LocalReferenceNumber}
import uk.gov.hmrc.customs.imports.repositories.SubmissionRepository

@Singleton
class ImportService @Inject()(submissionRepository: SubmissionRepository, customsDeclarationsConnector: CustomsDeclarationsConnector) {
  def handleDeclarationSubmit(eori: Eori, localReferenceNumber: LocalReferenceNumber) = ???


}
