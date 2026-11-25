import { Link, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ApplicationPage } from './pages/ApplicationPage'
import { ApplicationsPage } from './pages/ApplicationsPage'
import { LoginPage } from './pages/LoginPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Layout />}>
        <Route index element={<ApplicationsPage />} />
        <Route path="applications/:id" element={<ApplicationPage />} />
        <Route
          path="*"
          element={
            <p>
              Page not found. <Link to="/">Go to your applications</Link>
            </p>
          }
        />
      </Route>
    </Routes>
  )
}
