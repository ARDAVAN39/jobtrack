import { describe, expect, it, vi } from 'vitest'
import { mockFetch } from '../test/utils'
import { api, ApiError, request, setUnauthorizedHandler, tokenStore } from './client'

describe('request', () => {
  it('sends the stored token', async () => {
    tokenStore.set('abc')
    const fetch = mockFetch(() => ({ body: { ok: true } }))

    await request('/api/me')

    const headers = new Headers(fetch.mock.calls[0][1]?.headers)
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('turns a problem response into an ApiError with field errors', async () => {
    mockFetch(() => ({
      status: 400,
      body: { title: 'Bad Request', detail: 'Some fields are not valid', errors: { company: 'must not be blank' } },
    }))

    const error = await request('/api/applications').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Some fields are not valid', fieldErrors: { company: 'must not be blank' } })
  })

  it('signs the user out when the server rejects the token', async () => {
    tokenStore.set('expired')
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    mockFetch(() => ({ status: 401 }))

    await expect(request('/api/applications')).rejects.toBeInstanceOf(ApiError)

    expect(tokenStore.get()).toBeNull()
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('returns nothing for 204 No Content', async () => {
    mockFetch(() => ({ status: 204 }))

    await expect(request('/api/applications/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})

describe('api', () => {
  it('sends empty optional fields as null and trims text', async () => {
    const fetch = mockFetch(() => ({ status: 201, body: {} }))

    await api.createApplication(
      { company: '  Acme ', roleTitle: 'Backend Engineer', location: '', jobUrl: ' ', appliedOn: '', notes: 'Referral' },
      'APPLIED',
    )

    expect(JSON.parse(String(fetch.mock.calls[0][1]?.body))).toEqual({
      company: 'Acme',
      roleTitle: 'Backend Engineer',
      location: null,
      jobUrl: null,
      appliedOn: null,
      notes: 'Referral',
      status: 'APPLIED',
    })
  })

  it('only puts filters in the URL when they are set', async () => {
    const fetch = mockFetch(() => ({ body: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 } }))

    await api.listApplications({ status: '', q: '  ', page: 0 })
    await api.listApplications({ status: 'OFFER', q: 'acme', page: 2 })

    expect(fetch.mock.calls[0][0]).toBe('/api/applications?page=0')
    expect(fetch.mock.calls[1][0]).toBe('/api/applications?status=OFFER&q=acme&page=2')
  })
})
