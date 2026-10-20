import { useEffect, useState, type FormEvent } from 'react'
import './App.css'
import {
  clearAccessToken,
  createIncident,
  getAccessToken,
  getDashboardSummary,
  getIncidents,
  getIncidentUpdates,
  getIncidentHistory,
  addIncidentUpdate,
  login,
  getCurrentUser,
  getServices,
  transitionIncident,
  assignIncident,
  getAssignableUsers,
  type AssignableUser,
  type CurrentUser,
  type IncidentStatus,
  saveAccessToken,
  type DashboardSummary,
  type Incident,
  type IncidentUpdate,
  type IncidentHistoryItem,
  getIncidentById,
  type IncidentSeverity,
  type Service,
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

function formatHistoryType(type: string): string {
  const labels: Record<string, string> = {
    INCIDENT_CREATED: 'Incident created',
    INCIDENT_ASSIGNED: 'Assignee changed',
    SEVERITY_CHANGED: 'Severity changed',
    STATUS_CHANGED: 'Status changed',
    INVESTIGATION_UPDATE: 'Investigation update',
  }

  return labels[type] ?? type.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (char) => char.toUpperCase())
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
  const [services, setServices] = useState<Service[]>([])
  const [servicesLoading, setServicesLoading] = useState(false)
  const [servicesError, setServicesError] = useState<string | null>(null)
  const [createServiceId, setCreateServiceId] = useState('')
  const [createLoading, setCreateLoading] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [createSuccess, setCreateSuccess] = useState<string | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')

  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState<string | null>(null)
  const [incidentUpdates, setIncidentUpdates] = useState<IncidentUpdate[]>([])
  const [updatesLoading, setUpdatesLoading] = useState(false)
  const [updatesError, setUpdatesError] = useState<string | null>(null)
  const [updateMessage, setUpdateMessage] = useState('')
  const [updateSubmitting, setUpdateSubmitting] = useState(false)
  const [updateSubmitError, setUpdateSubmitError] = useState<string | null>(null)
  const [updateSubmitSuccess, setUpdateSubmitSuccess] = useState<string | null>(null)
  const [incidentHistory, setIncidentHistory] = useState<IncidentHistoryItem[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState<string | null>(null)

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [userLoading, setUserLoading] = useState(false)

  const [transitionLoading, setTransitionLoading] = useState(false)
  const [transitionError, setTransitionError] = useState<string | null>(null)
  const [transitionSuccess, setTransitionSuccess] = useState<string | null>(null)

  const [assignableUsers, setAssignableUsers] = useState<AssignableUser[]>([])
  const [assigneeSelection, setAssigneeSelection] = useState('')
  const [assigneesLoading, setAssigneesLoading] = useState(false)
  const [assignmentLoading, setAssignmentLoading] = useState(false)
  const [assignmentError, setAssignmentError] = useState<string | null>(null)
  const [assignmentSuccess, setAssignmentSuccess] = useState<string | null>(null)

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
    setCreateServiceId('')
    setServices([])
    setServicesError(null)
    setShowCreateForm(false)
    setSelectedIncident(null)
    setDetailLoading(false)
    setDetailError(null)
    setIncidentUpdates([])
    setUpdatesLoading(false)
    setUpdatesError(null)
    setUpdateMessage('')
    setUpdateSubmitting(false)
    setUpdateSubmitError(null)
    setUpdateSubmitSuccess(null)
    setIncidentHistory([])
    setHistoryLoading(false)
    setHistoryError(null)
    setPassword('')
    setCurrentUser(null)
    setUserLoading(false)
    setTransitionLoading(false)
    setTransitionError(null)
    setTransitionSuccess(null)
    setAssignableUsers([])
    setAssigneeSelection('')
    setAssigneesLoading(false)
    setAssignmentLoading(false)
    setAssignmentError(null)
    setAssignmentSuccess(null)
  }
  async function handleIncidentTransition(status: IncidentStatus) {
    if (!selectedIncident || transitionLoading) {
      return
    }

    setTransitionLoading(true)
    setTransitionError(null)
    setTransitionSuccess(null)

    try {
      const updatedIncident = await transitionIncident(
          selectedIncident.id,
          status,
      )

      setSelectedIncident(updatedIncident)

      setIncidents((current) =>
          current.map((incident) =>
              incident.id === updatedIncident.id ? updatedIncident : incident,
          ),
      )

      setTransitionSuccess(
          `Incident status changed to ${formatStatus(updatedIncident.status)}.`,
      )

      try {
        const history = await getIncidentHistory(updatedIncident.id)
        setIncidentHistory(history)
        setHistoryError(null)
      } catch (err) {
        setHistoryError(
            err instanceof Error
                ? err.message
                : 'Status changed, but incident history could not be refreshed.',
        )
      }
    } catch (err) {
      setTransitionError(
          err instanceof Error
              ? err.message
              : 'Unable to change incident status.',
      )
    } finally {
      setTransitionLoading(false)
    }
  }
  async function handleAssignIncident() {
    if (!selectedIncident || !assigneeSelection || assignmentLoading) {
      return
    }

    const userId = Number(assigneeSelection)

    if (!Number.isInteger(userId) || userId <= 0) {
      setAssignmentError('Select a valid assignee.')
      return
    }

    setAssignmentLoading(true)
    setAssignmentError(null)
    setAssignmentSuccess(null)

    try {
      const updatedIncident = await assignIncident(selectedIncident.id, userId)

      setSelectedIncident(updatedIncident)
      setIncidents((current) =>
          current.map((incident) =>
              incident.id === updatedIncident.id ? updatedIncident : incident,
          ),
      )

      setAssignmentSuccess('Incident assigned successfully.')

      try {
        const history = await getIncidentHistory(updatedIncident.id)
        setIncidentHistory(history)
        setHistoryError(null)
      } catch (err) {
        setHistoryError(
            err instanceof Error
                ? err.message
                : 'Incident was assigned, but history could not be refreshed.',
        )
      }
    } catch (err) {
      setAssignmentError(
          err instanceof Error ? err.message : 'Unable to assign incident.',
      )
    } finally {
      setAssignmentLoading(false)
    }
  }
  async function handleOpenIncident(id: number) {
    setDetailLoading(true)
    setDetailError(null)
    setAssignmentError(null)
    setAssignmentSuccess(null)
    setAssigneeSelection('')

    if (
        currentUser &&
        (currentUser.role === 'ADMIN' || currentUser.role === 'RESPONDER')
    ) {
      void loadAssignableUsers()
    } else {
      setAssignableUsers([])
      setAssigneesLoading(false)
    }

    setSelectedIncident(null)
    setIncidentUpdates([])
    setUpdatesLoading(true)
    setUpdatesError(null)
    setUpdateMessage('')
    setUpdateSubmitError(null)
    setUpdateSubmitSuccess(null)
    setIncidentHistory([])
    setHistoryLoading(true)
    setHistoryError(null)

    try {
      const incident = await getIncidentById(id)
      setSelectedIncident(incident)
    } catch (err) {
      setDetailError(
          err instanceof Error
              ? err.message
              : 'Unable to load incident details.',
      )
    } finally {
      setDetailLoading(false)
    }

    try {
      const updates = await getIncidentUpdates(id)
      setIncidentUpdates(updates)
    } catch (err) {
      setUpdatesError(
          err instanceof Error
              ? err.message
              : 'Unable to load investigation updates.',
      )
    } finally {
      setUpdatesLoading(false)
    }

    try {
      const history = await getIncidentHistory(id)
      setIncidentHistory(history)
    } catch (err) {
      setHistoryError(
          err instanceof Error ? err.message : 'Unable to load incident history.',
      )
    } finally {
      setHistoryLoading(false)
    }
  }

  async function handleAddIncidentUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setUpdateSubmitError(null)
    setUpdateSubmitSuccess(null)

    if (!selectedIncident) {
      return
    }

    const message = updateMessage.trim()

    if (!message) {
      setUpdateSubmitError('Enter an investigation update before submitting.')
      return
    }

    if (message.length > 5000) {
      setUpdateSubmitError('An update must not exceed 5,000 characters.')
      return
    }

    setUpdateSubmitting(true)

    try {
      const createdUpdate = await addIncidentUpdate(selectedIncident.id, message)
      setIncidentUpdates((currentUpdates) => [...currentUpdates, createdUpdate])
      setUpdateMessage('')
      setUpdateSubmitSuccess('Investigation update added successfully.')
      setUpdatesError(null)

      // The history endpoint also includes investigation updates.
      try {
        const history = await getIncidentHistory(selectedIncident.id)
        setIncidentHistory(history)
        setHistoryError(null)
      } catch (err) {
        setHistoryError(
            err instanceof Error ? err.message : 'Unable to refresh incident history.',
        )
      }
    } catch (err) {
      setUpdateSubmitError(
          err instanceof Error
              ? err.message
              : 'Unable to add the investigation update.',
      )
    } finally {
      setUpdateSubmitting(false)
    }
  }

  useEffect(() => {
    if (!token) {
      return
    }

    let active = true

    async function loadCurrentUser() {
      setUserLoading(true)

      try {
        const user = await getCurrentUser()

        if (active) {
          setCurrentUser(user)
        }
      } catch (err) {
        if (active) {
          setCurrentUser(null)
          console.error('Unable to load current user:', err)
        }
      } finally {
        if (active) {
          setUserLoading(false)
        }
      }
    }

    void loadCurrentUser()

    return () => {
      active = false
    }
  }, [token])


  useEffect(() => {
    if (!token) {
      return
    }

    let active = true

    async function loadServiceRegistry() {
      setServicesLoading(true)
      setServicesError(null)

      try {
        const result = await getServices()

        if (active) {
          setServices(result)
        }
      } catch (err) {
        if (active) {
          setServices([])
          setServicesError(
              err instanceof Error
                  ? err.message
                  : 'Unable to load services.',
          )
        }
      } finally {
        if (active) {
          setServicesLoading(false)
        }
      }
    }

    void loadServiceRegistry()

    return () => {
      active = false
    }
  }, [token])


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

  async function loadAssignableUsers() {
    setAssigneesLoading(true)
    setAssignmentError(null)

    try {
      const users = await getAssignableUsers()
      setAssignableUsers(users)
    } catch (err) {
      setAssignableUsers([])
      setAssignmentError(
          err instanceof Error
              ? err.message
              : 'Unable to load eligible assignees.',
      )
    } finally {
      setAssigneesLoading(false)
    }
  }
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
        ...(createServiceId ? { serviceId: Number(createServiceId) } : {}),
      })

      // Creation succeeded. Close the form and confirm the saved incident.
      setCreateTitle('')
      setCreateDescription('')
      setCreateSeverity('MEDIUM')
      setCreateServiceId('')
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


  if (selectedIncident || detailLoading || detailError) {
    return (
        <main className="main-content">
          <section className="page-content">
            <button
                type="button"
                className="secondary-button"
                onClick={() => {
                  setSelectedIncident(null)
                  setDetailError(null)
                  setIncidentUpdates([])
                  setUpdatesError(null)
                  setUpdateMessage('')
                  setUpdateSubmitError(null)
                  setUpdateSubmitSuccess(null)
                  setIncidentHistory([])
                  setHistoryError(null)
                }}
            >
              Back to dashboard
            </button>

            {detailLoading && <p>Loading incident details...</p>}

            {detailError && (
                <div role="alert" className="error-banner">
                  {detailError}
                </div>
            )}

            {selectedIncident && (
                <section className="incident-details">
                  <p className="incident-id">{formatIncidentId(selectedIncident.id)}</p>
                  <h1>{selectedIncident.title}</h1>
                  <p>{selectedIncident.description}</p>

                  <div className="incident-detail-layout">
                    {currentUser &&
                        (currentUser.role === 'ADMIN' || currentUser.role === 'RESPONDER') && (
                            <section className="incident-transition-panel" aria-labelledby="transition-heading">
                              <h2 id="transition-heading">Incident status</h2>

                              <p>
                                Current status: <strong>{formatStatus(selectedIncident.status)}</strong>
                              </p>

                              {selectedIncident.status === 'OPEN' && (
                                  <button
                                      className="primary-button"
                                      type="button"
                                      disabled={transitionLoading}
                                      onClick={() => void handleIncidentTransition('IN_PROGRESS')}
                                  >
                                    {transitionLoading ? 'Updating…' : 'Start investigation'}
                                  </button>
                              )}

                              {selectedIncident.status === 'IN_PROGRESS' && (
                                  <button
                                      className="primary-button"
                                      type="button"
                                      disabled={transitionLoading}
                                      onClick={() => void handleIncidentTransition('RESOLVED')}
                                  >
                                    {transitionLoading ? 'Updating…' : 'Resolve incident'}
                                  </button>
                              )}

                              {selectedIncident.status === 'RESOLVED' && (
                                  <button
                                      className="secondary-button"
                                      type="button"
                                      disabled={transitionLoading}
                                      onClick={() => void handleIncidentTransition('IN_PROGRESS')}
                                  >
                                    {transitionLoading ? 'Updating…' : 'Reopen incident'}
                                  </button>
                              )}

                              {transitionError && (
                                  <div className="error-banner" role="alert">
                                    {transitionError}
                                  </div>
                              )}

                              {transitionSuccess && (
                                  <div className="success-banner" role="status">
                                    {transitionSuccess}
                                  </div>
                              )}

                              {userLoading && <p>Checking permissions…</p>}
                            </section>
                        )}

                    <dl className="incident-metadata-grid">
                      <div className="incident-metadata-item">
                        <dt>Status</dt>
                        <dd>{formatStatus(selectedIncident.status)}</dd>
                      </div>

                      <div className="incident-metadata-item">
                        <dt>Severity</dt>
                        <dd>{selectedIncident.severity}</dd>
                      </div>

                      <div className="incident-metadata-item">
                        <dt>Service</dt>
                        <dd>{selectedIncident.serviceName ?? 'No service associated'}</dd>
                      </div>

                      <div className="incident-metadata-item">
                        <dt>Assignee</dt>
                        <dd>{formatAssignee(selectedIncident)}</dd>
                      </div>

                      <div className="incident-metadata-item">
                        <dt>Created</dt>
                        <dd>{new Date(selectedIncident.createdAt).toLocaleString()}</dd>
                      </div>

                      <div className="incident-metadata-item">
                        <dt>Last updated</dt>
                        <dd>{new Date(selectedIncident.updatedAt).toLocaleString()}</dd>
                      </div>
                    </dl>
                  </div>
                </section>
            )}

            {selectedIncident &&
                currentUser &&
                (currentUser.role === 'ADMIN' || currentUser.role === 'RESPONDER') && (
                    <section className="incident-transition-panel" aria-labelledby="assignment-heading">
                      <h2 id="assignment-heading">Assign incident</h2>

                      {assigneesLoading ? (
                          <p>Loading eligible assignees…</p>
                      ) : assignmentError && assignableUsers.length === 0 ? (
                          <p>Eligible assignees could not be loaded. See the error below and try again.</p>
                      ) : assignableUsers.length === 0 ? (
                          <p>No eligible assignees are available.</p>
                      ) : (
                          <form
                              onSubmit={(event) => {
                                event.preventDefault()
                                void handleAssignIncident()
                              }}
                          >
                            <label htmlFor="incident-assignee">Assignee</label>
                            <select
                                id="incident-assignee"
                                value={assigneeSelection}
                                onChange={(event) => {
                                  setAssigneeSelection(event.target.value)
                                  setAssignmentError(null)
                                  setAssignmentSuccess(null)
                                }}
                                disabled={assignmentLoading}
                                required
                            >
                              <option value="">Select an assignee</option>
                              {assignableUsers.map((user) => (
                                  <option key={user.id} value={user.id}>
                                    {user.displayName} ({user.role}) — {user.email}
                                  </option>
                              ))}
                            </select>

                            <button
                                type="submit"
                                className="primary-button"
                                disabled={assignmentLoading || !assigneeSelection}
                            >
                              {assignmentLoading ? 'Assigning…' : 'Assign incident'}
                            </button>
                          </form>
                      )}

                      {assignmentError && (
                          <div className="error-banner" role="alert">
                            <span>{assignmentError}</span>
                            {assignableUsers.length === 0 && !assigneesLoading && (
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => void loadAssignableUsers()}
                                >
                                  Retry loading assignees
                                </button>
                            )}
                          </div>
                      )}

                      {assignmentSuccess && (
                          <div className="success-banner" role="status">
                            {assignmentSuccess}
                          </div>
                      )}
                    </section>
                )}
            {selectedIncident && (
                <section className="incident-updates-panel" aria-labelledby="incident-updates-heading">
                  <div className="incident-updates-heading">
                    <div>
                      <h2 id="incident-updates-heading">Investigation updates</h2>
                      <p>Record findings, progress, and relevant investigation notes.</p>
                    </div>
                    <span className="incident-updates-count">{incidentUpdates.length}</span>
                  </div>

                  {updatesLoading ? (
                      <p className="incident-updates-message">Loading investigation updates…</p>
                  ) : updatesError ? (
                      <div className="error-banner" role="alert">
                        <span>{updatesError}</span>
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={async () => {
                              if (!selectedIncident) {
                                return
                              }
                              setUpdatesLoading(true)
                              setUpdatesError(null)
                              try {
                                const updates = await getIncidentUpdates(selectedIncident.id)
                                setIncidentUpdates(updates)
                              } catch (err) {
                                setUpdatesError(
                                    err instanceof Error
                                        ? err.message
                                        : 'Unable to load investigation updates.',
                                )
                              } finally {
                                setUpdatesLoading(false)
                              }
                            }}
                        >
                          Retry
                        </button>
                      </div>
                  ) : incidentUpdates.length === 0 ? (
                      <p className="incident-updates-empty">
                        No investigation updates yet. Add the first note below.
                      </p>
                  ) : (
                      <ol className="incident-updates-list">
                        {incidentUpdates.map((update) => (
                            <li className="incident-update-item" key={update.id}>
                              <div className="incident-update-meta">
                                <span>Investigation note</span>
                                <time dateTime={update.createdAt}>
                                  {new Date(update.createdAt).toLocaleString()}
                                </time>
                              </div>
                              <p>{update.message}</p>
                            </li>
                        ))}
                      </ol>
                  )}

                  <form className="incident-update-form" onSubmit={handleAddIncidentUpdate}>
                    <label htmlFor="incident-update-message">Add an investigation update</label>
                    <textarea
                        id="incident-update-message"
                        value={updateMessage}
                        onChange={(event) => {
                          setUpdateMessage(event.target.value)
                          setUpdateSubmitError(null)
                          setUpdateSubmitSuccess(null)
                        }}
                        placeholder="Record findings, actions taken, or the next investigation step…"
                        rows={5}
                        maxLength={5000}
                        required
                        disabled={updateSubmitting}
                    />
                    <div className="incident-update-form-footer">
                      <span>{updateMessage.length}/5000 characters</span>
                      <button
                          className="primary-button"
                          type="submit"
                          disabled={updateSubmitting || !updateMessage.trim() || updateMessage.length > 5000}
                      >
                        {updateSubmitting ? 'Saving update…' : 'Add update'}
                      </button>
                    </div>

                    {updateSubmitError && (
                        <div className="error-banner" role="alert">{updateSubmitError}</div>
                    )}
                    {updateSubmitSuccess && (
                        <div className="success-banner" role="status">{updateSubmitSuccess}</div>
                    )}
                  </form>
                </section>
            )}

            {selectedIncident && (
                <section className="incident-history-panel" aria-labelledby="incident-history-heading">
                  <div className="incident-history-heading">
                    <div>
                      <h2 id="incident-history-heading">Incident history</h2>
                      <p>Chronological record of important incident events.</p>
                    </div>
                    <span className="incident-history-count">{incidentHistory.length}</span>
                  </div>

                  {historyLoading ? (
                      <p className="incident-history-message">Loading incident history…</p>
                  ) : historyError ? (
                      <div className="error-banner" role="alert">
                        <span>{historyError}</span>
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={async () => {
                              setHistoryLoading(true)
                              setHistoryError(null)
                              try {
                                const history = await getIncidentHistory(selectedIncident.id)
                                setIncidentHistory(history)
                              } catch (err) {
                                setHistoryError(
                                    err instanceof Error ? err.message : 'Unable to load incident history.',
                                )
                              } finally {
                                setHistoryLoading(false)
                              }
                            }}
                        >
                          Retry
                        </button>
                      </div>
                  ) : incidentHistory.length === 0 ? (
                      <p className="incident-history-empty">No history events are available for this incident yet.</p>
                  ) : (
                      <ol className="incident-history-list">
                        {incidentHistory.map((item, index) => (
                            <li className="incident-history-item" key={`${item.createdAt}-${item.type}-${index}`}>
                              <span className="incident-history-marker" aria-hidden="true" />
                              <div className="incident-history-entry">
                                <div className="incident-history-meta">
                                  <strong>{formatHistoryType(item.type)}</strong>
                                  <time dateTime={item.createdAt}>
                                    {new Date(item.createdAt).toLocaleString()}
                                  </time>
                                </div>
                                <p>{item.details}</p>
                              </div>
                            </li>
                        ))}
                      </ol>
                  )}
                </section>
            )}
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

                    <label htmlFor="incident-service">Service (optional)</label>
                    <select
                        id="incident-service"
                        value={createServiceId}
                        onChange={(event) => setCreateServiceId(event.target.value)}
                        disabled={createLoading || servicesLoading}
                    >
                      <option value="">No service associated</option>
                      {services.map((service) => (
                          <option key={service.id} value={service.id}>
                            {service.name}
                          </option>
                      ))}
                    </select>
                    {servicesLoading && <small className="field-hint">Loading services…</small>}
                    {servicesError && (
                        <small className="field-hint" role="status">
                          Services could not be loaded. You can still create an incident without a service.
                        </small>
                    )}

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
                              <button
                                  className="incident-title-button"
                                  type="button"
                                  onClick={() => void handleOpenIncident(incident.id)}
                              >
                                {incident.title}
                              </button>
                              <div className="incident-id">
                                {formatIncidentId(incident.id)}
                              </div>
                            </td>

                            <td>
                              <span className="service-name">{incident.serviceName ?? '—'}</span>
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
