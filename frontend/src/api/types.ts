// Shapes of the JSON exchanged with the API. They mirror the DTOs in the backend.

export const STATUSES = ['WISHLIST', 'APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED', 'WITHDRAWN'] as const

export type ApplicationStatus = (typeof STATUSES)[number]

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  WISHLIST: 'Wishlist',
  APPLIED: 'Applied',
  INTERVIEWING: 'Interviewing',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
}

export interface User {
  id: number
  email: string
  displayName: string
}

export interface AuthResponse {
  token: string
  expiresAt: string
  user: User
}

export interface ApplicationSummary {
  id: number
  company: string
  roleTitle: string
  location: string | null
  status: ApplicationStatus
  appliedOn: string | null
  updatedAt: string
}

export interface StatusChange {
  from: ApplicationStatus | null
  to: ApplicationStatus
  note: string | null
  changedAt: string
}

export interface ApplicationDetail extends ApplicationSummary {
  jobUrl: string | null
  notes: string | null
  version: number
  createdAt: string
  history: StatusChange[]
}

/** The editable fields, as the form holds them. Empty strings mean "not set". */
export interface ApplicationFields {
  company: string
  roleTitle: string
  location: string
  jobUrl: string
  appliedOn: string
  notes: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface Stats {
  total: number
  byStatus: Record<ApplicationStatus, number>
  active: number
  interviewRate: number | null
}
