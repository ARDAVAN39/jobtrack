import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

/** The frame around every signed-in page. Sends signed-out visitors to the login page. */
export function Layout() {
  const { user, loading, logout } = useAuth()
  const location = useLocation()

  if (loading) {
    return <p className="centered">Loading…</p>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return (
    <>
      <header className="topbar">
        <Link to="/" className="brand">
          jobtrack
        </Link>
        <div className="topbar-user">
          <span>{user.displayName}</span>
          <button type="button" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  )
}
