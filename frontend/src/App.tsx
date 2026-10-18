
import { useEffect, useState, type FormEvent } from 'react'
import './App.css'
import {
  clearAccessToken,
  createIncident,
  getAccessToken,
  getDashboardSummary,
  getIncidents,
  login,
  saveAccessToken,
  type DashboardSummary,
  type Incident,
  type IncidentSeverity,
} from './api/devopsHubApi'

function formatStatus(status: Incident['status']): string {
  return status.replace('_', ' ')
}

function formatIncidentId(id: number): string {
  return `INC-${String(id).padStart(4, '0')}`
}

function formatAssignee(incident: Incident): string {
  return incident.assigneeEmail ?? 'Unassigned'
}

function App() {
  const [token, setToken] = useState<string | null>(() => getAccessToken())

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loginLoading, setLoginLoading] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)

  const [incidents, setIncidents] = useState<Incident[]>([])
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(false)
  const [dashboardError, setDashboardError] = useState<string | null>(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createTitle, setCreateTitle] = useState('')
  const [createDescription, setCreateDescription] = useState('')
  const [createSeverity, setCreateSeverity] =
      useState<IncidentSeverity>('MEDIUM')
  const [createLoading, setCreateLoading] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createSuccess, setCreateSuccess] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoginError(null)
    setLoginLoading(true)

    try {
      const response = await login({
        email: email.trim(),
        password,
      })

      if (
          !response.accessToken ||
          response.tokenType.toLowerCase() !== 'bearer'
      ) {
        throw new Error('The server returned an invalid login response.')
      }

      saveAccessToken(response.accessToken)
      setToken(response.accessToken)
      setPassword('')
    } catch (err) {
      setLoginError(
          err instanceof Error ? err.message : 'Unable to sign in.',
      )
    } finally {
      setLoginLoading(false)
    }
  }

  function handleLogout() {
    clearAccessToken()
    setToken(null)
    setIncidents([])
    setSummary(null)
    setDashboardError(null)
    setCreateError(null)
    setCreateSuccess(null)
    setShowCreateForm(false)
    setPassword('')
  }

  useEffect(() => {
    if (!token) {
      return
    }

    let active = true

    async function loadDashboard() {
      setLoading(true)
      setDashboardError(null)

      try {
        const [incidentPage, dashboardSummary] = await Promise.all([
          getIncidents(currentPage, 10, searchTerm),
          getDashboardSummary(),
        ])

        if (active) {
          setIncidents(incidentPage.content)
          setTotalPages(incidentPage.totalPages)
          setSummary(dashboardSummary)
        }
      } catch (err) {
        if (active) {
          const message =
              err instanceof Error
                  ? err.message
                  : 'Unable to load dashboard data.'

          setDashboardError(message)

          if (
              message.toLowerCase().includes('session is missing or expired')
          ) {
            clearAccessToken()
            setToken(null)
          }
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void loadDashboard()

    return () => {
      active = false
    }
  }, [token, currentPage, searchTerm])

  async function handleCreateIncident(
      event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()
    setCreateError(null)
    setCreateSuccess(null)

    const title = createTitle.trim()
    const description = createDescription.trim()

    if (!title || !description) {
      setCreateError('Title and description are required.')
      return
    }

    if (title.length > 200) {
      setCreateError('Title must not exceed 200 characters.')
      return
    }

    setCreateLoading(true)

    try {
      const created = await createIncident({
        title,
        description,
        severity: createSeverity,
      })

      // Creation succeeded. Close the form and confirm the saved incident.
      setCreateTitle('')
      setCreateDescription('')
      setCreateSeverity('MEDIUM')
      setShowCreateForm(false)
      setCreateSuccess(
          `Incident ${formatIncidentId(created.id)} created successfully.`,
      )

      // Refresh dashboard data independently of the successful creation.
      try {
        const [incidentPage, dashboardSummary] = await Promise.all([
          getIncidents(0, 10),
          getDashboardSummary(),
        ])

        setIncidents(incidentPage.content)
        setTotalPages(incidentPage.totalPages)
        setSummary(dashboardSummary)
        setDashboardError(null)
      } catch {
        setDashboardError(
            'The incident was created, but dashboard data could not be refreshed. Reload the page to try again.',
        )
      }
    } catch (err) {
      setCreateError(
          err instanceof Error
              ? err.message
              : 'Unable to create the incident.',
      )
    } finally {
      setCreateLoading(false)
    }
  }

  if (!token) {
    return (
        <main className="login-page">
          <section className="login-card">
            <a className="login-brand" href="#login">
              <span className="brand-mark">D</span>
              <span>
              <strong>DevOpsHub</strong>
              <small>ENGINEERING OPERATIONS</small>
            </span>
            </a>

            <div className="login-heading">
              <div className="eyebrow">WELCOME BACK</div>
              <h1>Sign in to DevOpsHub</h1>
              <p>
                Sign in to manage incidents and coordinate engineering
                investigations.
              </p>
            </div>

            {loginError && (
                <div className="login-error" role="alert">
                  {loginError}
                </div>
            )}

            <form className="login-form" onSubmit={handleLogin}>
              <label htmlFor="email">Email address</label>
              <input
                  id="email"
                  type="email"
                  autoComplete="username"
                  placeholder="you@company.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  maxLength={254}
                  required
                  disabled={loginLoading}
              />

              <label htmlFor="password">Password</label>
              <input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  maxLength={72}
                  required
                  disabled={loginLoading}
              />

              <button
                  className="primary-button login-submit"
                  type="submit"
                  disabled={loginLoading}
              >
                {loginLoading ? 'Signing in…' : 'Sign in'}
              </button>
            </form>

            <p className="login-footnote">
              Access is controlled by your DevOpsHub account permissions.
            </p>
          </section>
        </main>
    )
  }

  const metrics = [
    {
      label: 'Open incidents',
      value: summary?.openIncidents,
      note: 'Awaiting investigation',
      icon: '◷',
    },
    {
      label: 'In progress',
      value: summary?.inProgressIncidents,
      note: 'Currently being investigated',
      icon: '↗',
    },
    {
      label: 'Critical incidents',
      value: summary?.criticalSeverityIncidents,
      note: 'Require immediate attention',
      icon: '!',
    },
    {
      label: 'Resolved incidents',
      value: summary?.resolvedIncidents,
      note: 'Total resolved incidents',
      icon: '✓',
    },
  ]

  return (
      <div className="app-shell">
        <aside className="sidebar">
          <a className="brand" href="#dashboard" aria-label="DevOpsHub home">
            <span className="brand-mark">D</span>
            <span>
            <strong>DevOpsHub</strong>
            <small>ENGINEERING OPERATIONS</small>
          </span>
          </a>

          <div className="workspace-label">WORKSPACE</div>

          <nav className="navigation" aria-label="Main navigation">
            <a className="nav-item active" href="#dashboard">
              <span>▦</span> Dashboard
            </a>

            <a className="nav-item" href="#incidents">
              <span>◉</span> Incidents
              <span className="nav-count">
              {loading ? '—' : (summary?.openIncidents ?? 0)}
            </span>
            </a>

            <a className="nav-item" href="#services">
              <span>▤</span> Services
            </a>

            <a className="nav-item" href="#activity">
              <span>↻</span> Activity
            </a>
          </nav>

          <div className="sidebar-bottom">
            <div className="environment-card">
              <span className="environment-dot" />
              <div>
                <strong>Development</strong>
                <small>Local environment</small>
              </div>
            </div>

            <div className="profile">
              <div className="avatar">DH</div>
              <div className="profile-copy">
                <strong>{email || 'DevOpsHub User'}</strong>
                <small>Signed in</small>
              </div>
              <button
                  className="logout-button"
                  type="button"
                  onClick={handleLogout}
              >
                Sign out
              </button>
            </div>
          </div>
        </aside>

        <main className="main-content" id="dashboard">
          <header className="topbar">
            <div className="breadcrumb">
              Workspace <span>/</span> <strong>Dashboard</strong>
            </div>

            <div className="topbar-right">
            <span className="environment-badge">
              <span className="environment-dot" />
              Local environment
            </span>
              <div className="avatar top-avatar">DH</div>
            </div>
          </header>

          <section className="page-content">
            <div className="page-heading">
              <div>
                <div className="eyebrow">ENGINEERING OVERVIEW</div>
                <h1>Incident dashboard</h1>
                <p>
                  Monitor incidents, coordinate investigations, and track
                  resolution.
                </p>
              </div>

              <button
                  className="primary-button"
                  type="button"
                  onClick={() => {
                    setCreateError(null)
                    setCreateSuccess(null)
                    setShowCreateForm(true)
                  }}
              >
                <span>＋</span> Create incident
              </button>
            </div>

            {createSuccess && (
                <div className="success-banner" role="status">
                  <span>{createSuccess}</span>
                  <button
                      type="button"
                      onClick={() => setCreateSuccess(null)}
                      aria-label="Dismiss success message"
                  >
                    ×
                  </button>
                </div>
            )}

            {showCreateForm && (
                <section
                    className="create-panel"
                    aria-labelledby="create-incident-heading"
                >
                  <div className="create-panel-heading">
                    <div>
                      <h2 id="create-incident-heading">Create incident</h2>
                      <p>
                        Record an issue for your engineering team to investigate.
                      </p>
                    </div>

                    <button
                        className="secondary-button"
                        type="button"
                        disabled={createLoading}
                        onClick={() => {
                          setShowCreateForm(false)
                          setCreateError(null)
                        }}
                    >
                      Cancel
                    </button>
                  </div>

                  {createError && (
                      <div className="login-error" role="alert">
                        {createError}
                      </div>
                  )}

                  <form className="create-form" onSubmit={handleCreateIncident}>
                    <label htmlFor="incident-title">Title</label>
                    <input
                        id="incident-title"
                        value={createTitle}
                        onChange={(event) => setCreateTitle(event.target.value)}
                        maxLength={200}
                        placeholder="e.g. Payment API returning HTTP 500"
                        required
                        disabled={createLoading}
                    />
                    <small className="field-hint">
                      {createTitle.length}/200 characters
                    </small>

                    <label htmlFor="incident-description">Description</label>
                    <textarea
                        id="incident-description"
                        value={createDescription}
                        onChange={(event) =>
                            setCreateDescription(event.target.value)
                        }
                        placeholder="Describe the issue and its observed impact."
                        rows={5}
                        required
                        disabled={createLoading}
                    />

                    <label htmlFor="incident-severity">Severity</label>
                    <select
                        id="incident-severity"
                        value={createSeverity}
                        onChange={(event) =>
                            setCreateSeverity(
                                event.target.value as IncidentSeverity,
                            )
                        }
                        disabled={createLoading}
                    >
                      <option value="LOW">Low</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="HIGH">High</option>
                      <option value="CRITICAL">Critical</option>
                    </select>

                    <div className="create-form-actions">
                      <button
                          className="secondary-button"
                          type="button"
                          disabled={createLoading}
                          onClick={() => {
                            setShowCreateForm(false)
                            setCreateError(null)
                          }}
                      >
                        Cancel
                      </button>

                      <button
                          className="primary-button"
                          type="submit"
                          disabled={createLoading}
                      >
                        {createLoading ? 'Creating…' : 'Create incident'}
                      </button>
                    </div>
                  </form>
                </section>
            )}

            {dashboardError && (
                <div className="error-banner" role="alert">
                  <strong>Dashboard notice</strong>
                  <span>{dashboardError}</span>
                  <button
                      type="button"
                      onClick={() => window.location.reload()}
                  >
                    Retry
                  </button>
                </div>
            )}

            <div className="metrics-grid">
              {metrics.map((metric, index) => (
                  <article className="metric-card" key={metric.label}>
                    <div className="metric-top">
                      <span className="metric-label">{metric.label}</span>
                      <span className={`metric-icon metric-icon-${index}`}>
                    {metric.icon}
                  </span>
                    </div>

                    <div className="metric-value">
                      {loading ? '—' : (metric.value ?? '—')}
                    </div>

                    <div className="metric-note">{metric.note}</div>
                  </article>
              ))}
            </div>

            <section className="incidents-panel" id="incidents">
              <div className="panel-heading">
                <div>
                  <h2>Recent incidents</h2>
                  <p>Review and track the latest engineering incidents.</p>
                </div>

                <button className="secondary-button" type="button">
                  View all incidents <span>→</span>
                </button>
              </div>

              <div className="table-toolbar">
                <div className="search-box">
                  <span>⌕</span>
                  <input
                      aria-label="Search incidents"
                      placeholder="Search incidents..."
                      value={searchTerm}
                      onChange={(event) => {
                        setSearchTerm(event.target.value)
                        setCurrentPage(0)
                      }}
                  />
                </div>

                <button className="filter-button" type="button" disabled>
                  <span>☷</span> Filters
                </button>
              </div>

              <div className="table-scroll">
                <table className="incident-table">
                  <thead>
                  <tr>
                    <th>INCIDENT</th>
                    <th>SERVICE</th>
                    <th>SEVERITY</th>
                    <th>STATUS</th>
                    <th>ASSIGNEE</th>
                    <th />
                  </tr>
                  </thead>

                  <tbody>
                  {loading ? (
                      <tr>
                        <td colSpan={6}>Loading incidents…</td>
                      </tr>
                  ) : dashboardError ? (
                      <tr>
                        <td colSpan={6}>
                          Dashboard data may be incomplete. Check the notice above.
                        </td>
                      </tr>
                  ) : incidents.length === 0 ? (
                      <tr>
                        <td colSpan={6}>No incidents found.</td>
                      </tr>
                  ) : (
                      incidents.map((incident) => (
                          <tr key={incident.id}>
                            <td>
                              <div className="incident-title">
                                {incident.title}
                              </div>
                              <div className="incident-id">
                                {formatIncidentId(incident.id)}
                              </div>
                            </td>

                            <td>
                              <span className="service-name">—</span>
                            </td>

                            <td>
                          <span
                              className={`severity severity-${incident.severity.toLowerCase()}`}
                          >
                            <span className="severity-dot" />
                            {incident.severity}
                          </span>
                            </td>

                            <td>
                          <span
                              className={`status status-${incident.status.toLowerCase()}`}
                          >
                            {formatStatus(incident.status)}
                          </span>
                            </td>

                            <td>
                          <span className="assignee">
                            <span className="assignee-avatar">
                              {incident.assigneeEmail
                                  ? incident.assigneeEmail[0].toUpperCase()
                                  : '?'}
                            </span>
                            {formatAssignee(incident)}
                          </span>
                            </td>

                            <td>
                              <button
                                  className="row-menu"
                                  type="button"
                                  aria-label={`Actions for incident ${incident.id}`}
                                  disabled
                              >
                                ···
                              </button>
                            </td>
                          </tr>
                      ))
                  )}
                  </tbody>
                </table>
              </div>
              <div className="table-footer">
  <span>
    {loading
        ? 'Loading incidents…'
        : dashboardError
            ? 'Dashboard data may be incomplete'
            : `Showing ${incidents.length} of ${summary?.totalIncidents ?? '—'} incidents`}
  </span>

                <span>
    Page {totalPages === 0 ? 0 : currentPage + 1} of {totalPages}
  </span>

                <button
                    className="pagination-button"
                    type="button"
                    disabled={loading || currentPage === 0}
                    onClick={() => setCurrentPage(page => Math.max(0, page - 1))}
                >
                  Previous
                </button>

                <button
                    className="pagination-button"
                    type="button"
                    disabled={loading || currentPage + 1 >= totalPages}
                    onClick={() => setCurrentPage(page => page + 1)}
                >
                  Next
                </button>
              </div>
            </section>

            <footer className="page-footer">
              <span>DevOpsHub</span>
              <span>Incident management for engineering teams</span>
            </footer>
          </section>
        </main>
      </div>
  )
}

export default App
