import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { tokenStore } from '../api/client'
import { mockFetch, renderApp } from '../test/utils'
import { LoginPage } from './LoginPage'

const USER = { id: 1, email: 'ada@example.com', displayName: 'Ada' }

describe('LoginPage', () => {
  it('signs in, stores the token and leaves the page', async () => {
    const fetch = mockFetch(() => ({ body: { token: 'tok-123', expiresAt: '2030-01-01T00:00:00Z', user: USER } }))
    renderApp(<LoginPage />, { path: '/login', route: '/login' })

    await userEvent.type(screen.getByLabelText('Email'), 'ada@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'correct horse battery')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
    expect(tokenStore.get()).toBe('tok-123')
    expect(fetch.mock.calls[0][0]).toBe('/api/auth/login')
  })

  it('shows the server message when the password is wrong', async () => {
    mockFetch(() => ({ status: 401, body: { title: 'Unauthorized', detail: 'Invalid email or password' } }))
    renderApp(<LoginPage />, { path: '/login', route: '/login' })

    await userEvent.type(screen.getByLabelText('Email'), 'ada@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong password')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(tokenStore.get()).toBeNull()
  })

  it('checks the password length before creating an account', async () => {
    const fetch = mockFetch(() => ({ status: 201, body: {} }))
    renderApp(<LoginPage />, { path: '/login', route: '/login' })

    await userEvent.click(screen.getByRole('tab', { name: 'Create account' }))
    await userEvent.type(screen.getByLabelText('Name'), 'Ada')
    await userEvent.type(screen.getByLabelText('Email'), 'ada@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'short')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByRole('alert')).toHaveTextContent('at least 8 characters')
    expect(fetch).not.toHaveBeenCalled()
  })
})
