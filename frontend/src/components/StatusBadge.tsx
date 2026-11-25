import { STATUS_LABELS, type ApplicationStatus } from '../api/types'

export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}
