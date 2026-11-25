import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import { STATUSES, STATUS_LABELS, type ApplicationFields, type ApplicationStatus } from '../api/types'
import { ApplicationForm } from '../components/ApplicationForm'
import { StatsBar } from '../components/StatsBar'
import { StatusBadge } from '../components/StatusBadge'
import { useDebouncedValue } from './useDebouncedValue'

export function ApplicationsPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [status, setStatus] = useState<ApplicationStatus | ''>('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)
  const [newStatus, setNewStatus] = useState<ApplicationStatus>('APPLIED')
  const q = useDebouncedValue(search, 300)

  const stats = useQuery({ queryKey: ['stats'], queryFn: api.stats })
  const list = useQuery({
    queryKey: ['applications', { status, q, page }],
    queryFn: () => api.listApplications({ status, q, page }),
    // keep showing the old page while the next one loads, instead of flashing empty
    placeholderData: keepPreviousData,
  })

  const create = useMutation({
    mutationFn: (fields: ApplicationFields) => api.createApplication(fields, newStatus),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
      navigate(`/applications/${created.id}`)
    },
  })

  const resetPage = <T,>(setter: (value: T) => void) => (value: T) => {
    setter(value)
    setPage(0)
  }

  return (
    <>
      {stats.data && <StatsBar stats={stats.data} />}

      <div className="toolbar">
        <input
          type="search"
          placeholder="Search company or role"
          aria-label="Search company or role"
          value={search}
          onChange={(e) => resetPage(setSearch)(e.target.value)}
        />
        <select aria-label="Filter by status" value={status} onChange={(e) => resetPage(setStatus)(e.target.value as ApplicationStatus | '')}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
        <button type="button" className="primary" onClick={() => setAdding((open) => !open)}>
          {adding ? 'Close' : 'Add application'}
        </button>
      </div>

      {adding && (
        <section className="card">
          <h2>New application</h2>
          <label className="field">
            <span>Status</span>
            <select value={newStatus} onChange={(e) => setNewStatus(e.target.value as ApplicationStatus)}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </label>
          <ApplicationForm
            submitLabel="Add"
            submitting={create.isPending}
            serverErrors={create.error instanceof ApiError ? create.error.fieldErrors : undefined}
            onSubmit={(fields) => create.mutate(fields)}
            onCancel={() => setAdding(false)}
          />
          {create.error && !(create.error instanceof ApiError && Object.keys(create.error.fieldErrors).length) && (
            <p className="alert" role="alert">
              {create.error.message}
            </p>
          )}
        </section>
      )}

      {list.isError && (
        <p className="alert" role="alert">
          Could not load applications: {list.error.message}
        </p>
      )}

      {list.data && list.data.items.length === 0 && (
        <p className="empty">{q || status ? 'Nothing matches these filters.' : 'No applications yet. Add the first one.'}</p>
      )}

      {list.data && list.data.items.length > 0 && (
        <>
          <table className="applications">
            <thead>
              <tr>
                <th>Company</th>
                <th>Role</th>
                <th>Location</th>
                <th>Status</th>
                <th>Applied</th>
              </tr>
            </thead>
            <tbody>
              {list.data.items.map((application) => (
                <tr key={application.id}>
                  <td>
                    <Link to={`/applications/${application.id}`}>{application.company}</Link>
                  </td>
                  <td>{application.roleTitle}</td>
                  <td>{application.location ?? '–'}</td>
                  <td>
                    <StatusBadge status={application.status} />
                  </td>
                  <td>{application.appliedOn ?? '–'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {list.data.totalPages > 1 && (
            <nav className="pager" aria-label="Pages">
              <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </button>
              <span>
                Page {page + 1} of {list.data.totalPages}
              </span>
              <button type="button" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage((p) => p + 1)}>
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </>
  )
}
