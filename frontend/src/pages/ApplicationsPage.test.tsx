import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { tokenStore } from '../api/client'
import type { ApplicationSummary, Stats } from '../api/types'
import { mockFetch, renderApp } from '../test/utils'
import { ApplicationsPage } from './ApplicationsPage'

const STATS: Stats = {
  total: 2,
  active: 2,
  interviewRate: 50,
  byStatus: { WISHLIST: 0, APPLIED: 1, INTERVIEWING: 1, OFFER: 0, REJECTED: 0, WITHDRAWN: 0 },
}

const ITEMS: ApplicationSummary[] = [
  { id: 1, company: 'Acme', roleTitle: 'Backend Engineer', location: 'Amsterdam', status: 'INTERVIEWING', appliedOn: '2026-11-02', updatedAt: '2026-11-10T09:00:00Z' },
  { id: 2, company: 'Globex', roleTitle: 'Java Developer', location: null, status: 'APPLIED', appliedOn: '2026-11-05', updatedAt: '2026-11-06T09:00:00Z' },
]

function serveApi() {
  return mockFetch((url) => {
    if (url === '/api/me') return { body: { id: 1, email: 'ada@example.com', displayName: 'Ada' } }
    if (url === '/api/applications/stats') return { body: STATS }
    if (url.startsWith('/api/applications?')) {
      const q = new URLSearchParams(url.split('?')[1]).get('q')
      const items = q ? ITEMS.filter((a) => a.company.toLowerCase().includes(q.toLowerCase())) : ITEMS
      return { body: { items, page: 0, size: 20, totalItems: items.length, totalPages: 1 } }
    }
    return { status: 404, body: { detail: 'not mocked: ' + url } }
  })
}

describe('ApplicationsPage', () => {
  it('shows the summary and the applications', async () => {
    tokenStore.set('tok')
    serveApi()
    renderApp(<ApplicationsPage />)

    const table = await screen.findByRole('table')
    expect(within(table).getByRole('link', { name: 'Acme' })).toHaveAttribute('href', '/applications/1')
    expect(within(table).getByText('Interviewing')).toBeInTheDocument()
    expect(within(table).getByText('Globex')).toBeInTheDocument()

    const summary = screen.getByRole('region', { name: 'Summary' })
    expect(within(summary).getByText('50%')).toBeInTheDocument()
  })

  it('searches as the user types', async () => {
    tokenStore.set('tok')
    const fetch = serveApi()
    renderApp(<ApplicationsPage />)
    await screen.findByRole('table')

    await userEvent.type(screen.getByLabelText('Search company or role'), 'glob')

    expect(await screen.findByText('Globex')).toBeInTheDocument()
    await screen.findByText((_, el) => el?.tagName === 'TBODY' && el.querySelectorAll('tr').length === 1)
    expect(fetch.mock.calls.some(([url]) => String(url).includes('q=glob'))).toBe(true)
  })

  it('says so when there is nothing yet', async () => {
    tokenStore.set('tok')
    mockFetch((url) =>
      url === '/api/applications/stats'
        ? { body: { ...STATS, total: 0, active: 0, interviewRate: null } }
        : { body: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 } },
    )
    renderApp(<ApplicationsPage />)

    expect(await screen.findByText('No applications yet. Add the first one.')).toBeInTheDocument()
  })
})
