import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

type Mode = 'login' | 'register'

export function LoginPage() {
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (mode === 'register' && password.length < 8) {
      setError('Use at least 8 characters for the password')
      return
    }
    setSubmitting(true)
    try {
      if (mode === 'login') {
        await login(email, password)
      } else {
        await register(email, password, displayName)
      }
      navigate(from, { replace: true })
    } catch (e) {
      setError(messageFor(e))
    } finally {
      setSubmitting(false)
    }
  }

  function switchMode(next: Mode) {
    setMode(next)
    setError(null)
  }

  return (
    <main className="auth">
      <h1 className="brand">jobtrack</h1>
      <p className="muted">Keep track of every job you apply to.</p>

      <div className="tabs" role="tablist">
        <button type="button" role="tab" aria-selected={mode === 'login'} onClick={() => switchMode('login')}>
          Sign in
        </button>
        <button type="button" role="tab" aria-selected={mode === 'register'} onClick={() => switchMode('register')}>
          Create account
        </button>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        {mode === 'register' && (
          <label className="field">
            <span>Name</span>
            <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required autoComplete="name" />
          </label>
        )}
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
        </label>
        {error && (
          <p className="alert" role="alert">
            {error}
          </p>
        )}
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
        </button>
      </form>
    </main>
  )
}

function messageFor(error: unknown): string {
  if (error instanceof ApiError) {
    const firstFieldError = Object.entries(error.fieldErrors)[0]
    return firstFieldError ? `${firstFieldError[0]}: ${firstFieldError[1]}` : error.message
  }
  return 'Could not reach the server. Try again in a moment.'
}
