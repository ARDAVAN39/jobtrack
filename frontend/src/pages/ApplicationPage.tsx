import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import { STATUSES, STATUS_LABELS, type ApplicationDetail, type ApplicationFields, type ApplicationStatus } from '../api/types'
import { ApplicationForm } from '../components/ApplicationForm'
import { StatusBadge } from '../components/StatusBadge'

export function ApplicationPage() {
  const id = Number(useParams().id)
  const application = useQuery({
    queryKey: ['application', id],
    queryFn: () => api.getApplication(id),
    enabled: Number.isInteger(id),
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 2,
  })

  if (application.isPending) {
    return <p>Loading…</p>
  }
  if (application.isError) {
    const notFound = application.error instanceof ApiError && application.error.status === 404
    return (
      <>
        <p className="alert" role="alert">
          {notFound ? 'This application does not exist.' : application.error.message}
        </p>
        <Link to="/">Back to all applications</Link>
      </>
    )
  }
  // keyed by version so the form starts from fresh data after every save
  return <ApplicationView key={application.data.version} application={application.data} />
}

function ApplicationView({ application }: { application: ApplicationDetail }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [nextStatus, setNextStatus] = useState<ApplicationStatus>(application.status)
  const [note, setNote] = useState('')

  const saved = (updated: ApplicationDetail) => {
    queryClient.setQueryData(['application', updated.id], updated)
    queryClient.invalidateQueries({ queryKey: ['applications'] })
    queryClient.invalidateQueries({ queryKey: ['stats'] })
  }

  const update = useMutation({
    mutationFn: (fields: ApplicationFields) => api.updateApplication(application.id, fields, application.version),
    onSuccess: (updated) => {
      saved(updated)
      setEditing(false)
    },
  })

  const changeStatus = useMutation({
    mutationFn: () => api.changeStatus(application.id, nextStatus, note),
    onSuccess: saved,
  })

  const remove = useMutation({
    mutationFn: () => api.deleteApplication(application.id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['application', application.id] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
      navigate('/')
    },
  })

  const conflict = update.error instanceof ApiError && update.error.status === 409

  return (
    <>
      <p>
        <Link to="/">← All applications</Link>
      </p>

      <header className="detail-header">
        <div>
          <h1>{application.company}</h1>
          <p className="muted">
            {application.roleTitle}
            {application.location && ` · ${application.location}`}
          </p>
        </div>
        <StatusBadge status={application.status} />
      </header>

      <div className="detail-grid">
        <section className="card">
          <h2>Details</h2>
          {editing ? (
            <>
              <ApplicationForm
                initial={toFields(application)}
                submitLabel="Save"
                submitting={update.isPending}
                serverErrors={update.error instanceof ApiError ? update.error.fieldErrors : undefined}
                onSubmit={(fields) => update.mutate(fields)}
                onCancel={() => setEditing(false)}
              />
              {conflict && (
                <p className="alert" role="alert">
                  {update.error?.message}{' '}
                  <button type="button" onClick={() => queryClient.invalidateQueries({ queryKey: ['application', application.id] })}>
                    Reload
                  </button>
                </p>
              )}
            </>
          ) : (
            <>
              <dl className="details">
                <dt>Applied on</dt>
                <dd>{application.appliedOn ?? '–'}</dd>
                <dt>Job posting</dt>
                <dd>
                  {application.jobUrl ? (
                    <a href={application.jobUrl} target="_blank" rel="noreferrer noopener">
                      Open posting
                    </a>
                  ) : (
                    '–'
                  )}
                </dd>
                <dt>Notes</dt>
                <dd className="notes">{application.notes ?? '–'}</dd>
              </dl>
              <div className="form-actions">
                <button type="button" onClick={() => setEditing(true)}>
                  Edit
                </button>
                <button
                  type="button"
                  className="danger"
                  disabled={remove.isPending}
                  onClick={() => window.confirm(`Delete the application to ${application.company}?`) && remove.mutate()}
                >
                  Delete
                </button>
              </div>
            </>
          )}
        </section>

        <section className="card">
          <h2>Status</h2>
          <form
            className="form"
            onSubmit={(e) => {
              e.preventDefault()
              changeStatus.mutate()
            }}
          >
            <label className="field">
              <span>Move to</span>
              <select value={nextStatus} onChange={(e) => setNextStatus(e.target.value as ApplicationStatus)}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Note (optional)</span>
              <input value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} placeholder="e.g. First call with HR" />
            </label>
            <button type="submit" className="primary" disabled={nextStatus === application.status || changeStatus.isPending}>
              Update status
            </button>
            {changeStatus.error && (
              <p className="alert" role="alert">
                {changeStatus.error.message}
              </p>
            )}
          </form>

          <h3>History</h3>
          <ol className="timeline">
            {[...application.history].reverse().map((change, index) => (
              <li key={`${change.changedAt}-${index}`}>
                <StatusBadge status={change.to} />
                <time dateTime={change.changedAt}>{new Date(change.changedAt).toLocaleString()}</time>
                {change.note && <p>{change.note}</p>}
              </li>
            ))}
          </ol>
        </section>
      </div>
    </>
  )
}

function toFields(application: ApplicationDetail): ApplicationFields {
  return {
    company: application.company,
    roleTitle: application.roleTitle,
    location: application.location ?? '',
    jobUrl: application.jobUrl ?? '',
    appliedOn: application.appliedOn ?? '',
    notes: application.notes ?? '',
  }
}
