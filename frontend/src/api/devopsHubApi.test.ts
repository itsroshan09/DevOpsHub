
import {
    afterEach,
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import {
    addIncidentUpdate,
    assignIncident,
    clearAccessToken,
    createIncident,
    getAccessToken,
    getAssignableUsers,
    getCurrentUser,
    getDashboardSummary,
    getIncidentById,
    getIncidentHistory,
    getIncidentUpdates,
    getIncidents,
    getServices,
    logout,
    refreshSession,
    saveAccessToken,
    transitionIncident,
} from './devopsHubApi'

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(
    body: unknown,
    status = 200,
): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: {
            'Content-Type': 'application/json',
        },
    })
}

function lastRequest() {
    const call = fetchMock.mock.calls.at(-1)

    if (!call) {
        throw new Error('Expected fetch to have been called')
    }

    const [url, options] = call

    return {
        url: String(url),
        options: options ?? {},
        headers: new Headers(options?.headers),
        body: options?.body
            ? JSON.parse(String(options.body))
            : undefined,
    }
}



describe('DevOpsHub API client', () => {
    beforeEach(() => {
        clearAccessToken()
        fetchMock.mockReset()

        vi.stubGlobal('fetch', fetchMock)

        fetchMock.mockResolvedValue(jsonResponse({}))
    })

    afterEach(() => {
        vi.unstubAllGlobals()
        vi.restoreAllMocks()
    })

    describe('access token storage', () => {
        it('returns null when no token exists', () => {
            expect(getAccessToken()).toBeNull()
        })

        it('saves and retrieves an access token in memory', () => {
            saveAccessToken('test-token')

            expect(getAccessToken()).toBe('test-token')
        })

        it('clears the in-memory access token', () => {
            saveAccessToken('test-token')

            clearAccessToken()

            expect(getAccessToken()).toBeNull()
        })
    })


    describe('refresh and logout', () => {
        it('refreshes the session using CSRF protection', async () => {
            fetchMock
                .mockResolvedValueOnce(
                    jsonResponse({
                        headerName: 'X-XSRF-TOKEN',
                        token: 'refresh-csrf-token',
                    }),
                )
                .mockResolvedValueOnce(
                    jsonResponse({
                        accessToken: 'refreshed-access-token',
                        tokenType: 'Bearer',
                        expiresIn: 900,
                    }),
                )

            const result = await refreshSession()

            expect(result.accessToken).toBe('refreshed-access-token')
            expect(fetchMock).toHaveBeenCalledTimes(2)

            const [csrfUrl] = fetchMock.mock.calls[0]
            expect(String(csrfUrl)).toBe('/api/auth/csrf')

            const request = lastRequest()

            expect(request.url).toBe('/api/auth/refresh')
            expect(request.options.method).toBe('POST')
            expect(request.headers.get('X-XSRF-TOKEN')).toBe(
                'refresh-csrf-token',
            )
            expect(request.headers.has('Authorization')).toBe(false)
        })

        it('logs out using CSRF protection and clears the access token', async () => {
            saveAccessToken('current-access-token')

            fetchMock
                .mockResolvedValueOnce(
                    jsonResponse({
                        headerName: 'X-XSRF-TOKEN',
                        token: 'logout-csrf-token',
                    }),
                )
                .mockResolvedValueOnce(
                    new Response(null, { status: 204 }),
                )

            await logout()

            expect(fetchMock).toHaveBeenCalledTimes(2)

            const request = lastRequest()

            expect(request.url).toBe('/api/auth/logout')
            expect(request.options.method).toBe('POST')
            expect(request.headers.get('X-XSRF-TOKEN')).toBe(
                'logout-csrf-token',
            )
            expect(request.headers.has('Authorization')).toBe(false)
            expect(getAccessToken()).toBeNull()
        })
    })


    describe('current user and services', () => {
        it('requests the current authenticated user', async () => {
            await getCurrentUser()

            expect(lastRequest().url).toBe('/api/users/me')
        })

        it('requests the service collection', async () => {
            const services = [
                {
                    id: 7,
                    name: 'Payments',
                    description: 'Payment processing',
                    createdAt: '2026-10-25T10:00:00Z',
                    updatedAt: '2026-10-25T10:00:00Z',
                },
            ]

            fetchMock.mockResolvedValue(jsonResponse(services))

            await expect(getServices()).resolves.toEqual(services)
            expect(lastRequest().url).toBe('/api/services')
        })

        it('requests assignable users', async () => {
            fetchMock.mockResolvedValue(
                jsonResponse([
                    {
                        id: 2,
                        email: 'responder@example.com',
                        displayName: 'Test Responder',
                        role: 'RESPONDER',
                    },
                ]),
            )

            const users = await getAssignableUsers()

            expect(users).toHaveLength(1)
            expect(lastRequest().url).toBe(
                '/api/users/assignable',
            )
        })
    })

    describe('incident retrieval and search', () => {
        it('uses default pagination values', async () => {
            await getIncidents()

            expect(lastRequest().url).toBe(
                '/api/incidents?page=0&size=10',
            )
        })

        it('sends pagination and trimmed search parameters', async () => {
            await getIncidents(2, 25, '  payment failure  ')

            const url = new URL(
                lastRequest().url,
                'http://localhost',
            )

            expect(url.pathname).toBe('/api/incidents')
            expect(url.searchParams.get('page')).toBe('2')
            expect(url.searchParams.get('size')).toBe('25')
            expect(url.searchParams.get('search')).toBe(
                'payment failure',
            )
        })

        it('omits the search parameter when search is blank', async () => {
            await getIncidents(0, 10, '   ')

            const url = new URL(
                lastRequest().url,
                'http://localhost',
            )

            expect(url.searchParams.has('search')).toBe(false)
        })

        it('retrieves an incident by ID', async () => {
            fetchMock.mockResolvedValue(
                jsonResponse({
                    id: 42,
                    title: 'API outage',
                }),
            )

            const incident = await getIncidentById(42)

            expect(incident.id).toBe(42)
            expect(lastRequest().url).toBe('/api/incidents/42')
        })
    })

    describe('incident creation and transitions', () => {
        it('creates an incident with the expected JSON payload', async () => {
            const payload = {
                title: 'API integration test',
                description: 'Testing incident creation.',
                severity: 'HIGH' as const,
                serviceId: 7,
            }

            fetchMock.mockResolvedValue(
                jsonResponse(
                    {
                        id: 42,
                        ...payload,
                        status: 'OPEN',
                        assigneeId: null,
                        assigneeEmail: null,
                        serviceName: 'Payments',
                        createdAt: '2026-10-25T10:00:00Z',
                        updatedAt: '2026-10-25T10:00:00Z',
                    },
                    201,
                ),
            )

            const incident = await createIncident(payload)
            const request = lastRequest()

            expect(incident.id).toBe(42)
            expect(request.url).toBe('/api/incidents')
            expect(request.options.method).toBe('POST')
            expect(request.body).toEqual(payload)
            expect(
                request.headers.get('Content-Type'),
            ).toBe('application/json')
        })

        it('creates an incident without an optional service ID', async () => {
            const payload = {
                title: 'Unassigned service incident',
                description: 'Testing optional service.',
                severity: 'MEDIUM' as const,
            }

            await createIncident(payload)

            expect(lastRequest().body).toEqual(payload)
            expect(lastRequest().body.serviceId).toBeUndefined()
        })

        it('sends a status transition request', async () => {
            await transitionIncident(42, 'IN_PROGRESS')

            const request = lastRequest()

            expect(request.url).toBe(
                '/api/incidents/42/transitions',
            )
            expect(request.options.method).toBe('POST')
            expect(request.body).toEqual({
                status: 'IN_PROGRESS',
            })
        })

        it('sends an incident assignment request', async () => {
            await assignIncident(42, 5)

            const request = lastRequest()

            expect(request.url).toBe(
                '/api/incidents/42/assignee',
            )
            expect(request.options.method).toBe('PUT')
            expect(request.body).toEqual({ userId: 5 })
        })
    })

    describe('incident investigation and history', () => {
        it('retrieves investigation updates for an incident', async () => {
            fetchMock.mockResolvedValue(
                jsonResponse([
                    {
                        id: 3,
                        incidentId: 42,
                        message: 'Investigating database latency',
                        createdAt: '2026-10-25T10:00:00Z',
                    },
                ]),
            )

            const updates = await getIncidentUpdates(42)

            expect(updates).toHaveLength(1)
            expect(lastRequest().url).toBe(
                '/api/incidents/42/updates',
            )
        })

        it('creates an investigation update', async () => {
            await addIncidentUpdate(
                42,
                'Restarted the affected service.',
            )

            const request = lastRequest()

            expect(request.url).toBe(
                '/api/incidents/42/updates',
            )
            expect(request.options.method).toBe('POST')
            expect(request.body).toEqual({
                message: 'Restarted the affected service.',
            })
        })

        it('retrieves incident history', async () => {
            fetchMock.mockResolvedValue(
                jsonResponse([
                    {
                        type: 'STATUS_CHANGED',
                        details: 'OPEN -> IN_PROGRESS',
                        createdAt: '2026-10-25T10:00:00Z',
                    },
                ]),
            )

            const history = await getIncidentHistory(42)

            expect(history).toHaveLength(1)
            expect(lastRequest().url).toBe(
                '/api/incidents/42/history',
            )
        })
    })

    describe('dashboard summary', () => {
        it('retrieves dashboard statistics', async () => {
            const summary = {
                totalIncidents: 12,
                openIncidents: 4,
                inProgressIncidents: 3,
                resolvedIncidents: 5,
                lowSeverityIncidents: 2,
                mediumSeverityIncidents: 4,
                highSeverityIncidents: 3,
                criticalSeverityIncidents: 3,
            }

            fetchMock.mockResolvedValue(jsonResponse(summary))

            await expect(
                getDashboardSummary(),
            ).resolves.toEqual(summary)

            expect(lastRequest().url).toBe(
                '/api/dashboard/summary',
            )
        })
    })
})
