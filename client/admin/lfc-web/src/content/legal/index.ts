import type { LegalDocument } from './types'

export type { LegalDocument, LegalSection } from './types'

export { PRIVACY_POLICY } from '../privacyPolicy'
export { C2C_AFTER_SALES } from './c2cAfterSales'
export { C2C_SECURITY } from './c2cSecurity'
export { PROMOTION_ADS } from './promotionAds'

import { PRIVACY_POLICY } from '../privacyPolicy'
import { C2C_AFTER_SALES } from './c2cAfterSales'
import { C2C_SECURITY } from './c2cSecurity'
import { PROMOTION_ADS } from './promotionAds'

export const PUBLIC_LEGAL_DOCUMENTS: LegalDocument[] = [
  PRIVACY_POLICY,
  C2C_SECURITY,
  C2C_AFTER_SALES,
  PROMOTION_ADS,
]

export function getLegalDocumentBySlug(slug: string): LegalDocument | undefined {
  return PUBLIC_LEGAL_DOCUMENTS.find((doc) => doc.slug === slug)
}
