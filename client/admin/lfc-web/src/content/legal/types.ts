export interface LegalSection {
  heading: string
  paragraphs: string[]
}

export interface LegalDocument {
  title: string
  appName: string
  updatedAt: string
  slug: string
  description: string
  sections: LegalSection[]
}
