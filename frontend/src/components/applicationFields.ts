import type { ApplicationFields } from '../api/types'

export const EMPTY_FIELDS: ApplicationFields = {
  company: '',
  roleTitle: '',
  location: '',
  jobUrl: '',
  appliedOn: '',
  notes: '',
}

export function validate(fields: ApplicationFields): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!fields.company.trim()) errors.company = 'Company is required'
  if (!fields.roleTitle.trim()) errors.roleTitle = 'Role is required'
  if (fields.jobUrl.trim() && !/^https?:\/\/.+/.test(fields.jobUrl.trim())) {
    errors.jobUrl = 'Must start with http:// or https://'
  }
  if (fields.appliedOn && fields.appliedOn > today()) {
    errors.appliedOn = 'Cannot be in the future'
  }
  return errors
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}
