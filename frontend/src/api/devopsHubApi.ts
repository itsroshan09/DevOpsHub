const API_BASE = '/api'
//const TOKEN_KEY = 'devopshub_access_token'
let accessToken: string | null = null
export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'

export type UserRole = 'ADMIN' | 'RESPONDER' | 'VIEWER'

export interface Incident {
    id: number
    title: string
    description: string
    severity: IncidentSeverity
    status: IncidentStatus
    createdAt: string
    updatedAt: string
    assigneeId: number | null
    assigneeEmail: string | null
    serviceId: number | null
    serviceName: string | null
}

export interface Service {
    id: number
    name: string
    description: string
    createdAt: string
    updatedAt: string
}

export interface IncidentUpdate {
    id: number
    incidentId: number
    message: string
    createdAt: string
}

export interface IncidentHistoryItem {
    type: string
    details: string
    createdAt: string
}

export interface CreateIncidentRequest {
    title: string
    description: string
    severity: IncidentSeverity
    serviceId?: number
}

export interface IncidentPage {
    content: Incident[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface CurrentUser {
    id: number
    email: string
    displayName: string
    role: UserRole
    createdAt: string
}

export type AssignableUser = Pick<
    CurrentUser,
    'id' | 'email' | 'displayName' | 'role'
>

export interface DashboardSummary {
    totalIncidents: number
    openIncidents: number
    inProgressIncidents: number
    resolvedIncidents: number
    lowSeverityIncidents: number
    mediumSeverityIncidents: number
    highSeverityIncidents: number
    criticalSeverityIncidents: number
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    accessToken: string
    tokenType: string
    expiresIn: number
}

interface CsrfResponse {
    headerName: string
    token: string
}

async function fetchCsrfToken(): Promise<CsrfResponse> {
    const response = await fetch(`${API_BASE}/auth/csrf`, {
        method: 'GET',
        headers: {
            Accept: 'application/json',
        },
    })

    if (!response.ok) {
        throw new Error(
            `Unable to initialize security token (HTTP ${response.status}).`,
        )
    }

    return response.json() as Promise<CsrfResponse>
}

export function getAccessToken(): string | null {
    return accessToken
}

export function saveAccessToken(token: string): void {
    accessToken = token
}

export function clearAccessToken(): void {
    accessToken = null
}


async function request<T>(
    path: string,
    options: RequestInit = {},
    authenticated = true,
    extraHeaders?: HeadersInit,
): Promise<T> {
    const headers = new Headers(options.headers)

    new Headers(extraHeaders).forEach((value, key) => {
        headers.set(key, value)
    })

    headers.set('Accept', 'application/json')

    if (options.body) {
        headers.set('Content-Type', 'application/json')
    }

    if (authenticated) {
        const token = getAccessToken()

        if (token) {
            headers.set('Authorization', `Bearer ${token}`)
        }
    }

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,
    })

    if (!response.ok) {
        if (response.status === 401) {
            throw new Error(
                authenticated
                    ? 'Your session is missing or expired. Please sign in again.'
                    : 'Authentication failed. Please try again.',
            )
        }

        if (response.status === 403) {
            throw new Error(
                'You do not have permission to perform this action.',
            )
        }

        const body = await response.text()

        throw new Error(
            body || `Request failed with HTTP ${response.status}.`,
        )
    }

    if (response.status === 204) {
        return undefined as T
    }

    return response.json() as Promise<T>
}



export async function login(
    credentials: LoginRequest,
): Promise<LoginResponse> {
    const csrf = await fetchCsrfToken()

    return request<LoginResponse>(
        '/auth/login',
        {
            method: 'POST',
            body: JSON.stringify(credentials),
        },
        false,
        { [csrf.headerName]: csrf.token },
    )
}



let refreshInFlight: Promise<LoginResponse> | null = null

export function refreshSession(): Promise<LoginResponse> {
    if (refreshInFlight) {
        return refreshInFlight
    }

    const request = navigator.locks.request(
        'devopshub-refresh-token',
        async () => {
            // Fetch CSRF only after acquiring the cross-tab lock.
            const csrf = await fetchCsrfToken()

            // The browser sends the latest cookie after the
            // previous tab has completed its refresh.
            return requestLoginRefresh(csrf)
        },
    )

    refreshInFlight = request

    void request.then(
        () => {
            if (refreshInFlight === request) {
                refreshInFlight = null
            }
        },
        () => {
            if (refreshInFlight === request) {
                refreshInFlight = null
            }
        },
    )

    return request
}

async function requestLoginRefresh(
    csrf: CsrfResponse,
): Promise<LoginResponse> {
    return request<LoginResponse>(
        '/auth/refresh',
        {
            method: 'POST',
        },
        false,
        { [csrf.headerName]: csrf.token },
    )
}


export async function logout(): Promise<void> {
    const csrf = await fetchCsrfToken()

    await request<void>(
        '/auth/logout',
        {
            method: 'POST',
        },
        false,
        { [csrf.headerName]: csrf.token },
    )

    clearAccessToken()
}


export function getCurrentUser(): Promise<CurrentUser> {
    return request<CurrentUser>('/users/me')
}

export function getServices(): Promise<Service[]> {
    return request<Service[]>('/services')
}

export function getAssignableUsers(): Promise<AssignableUser[]> {
    return request<AssignableUser[]>('/users/assignable')
}

export function getIncidents(
    page = 0,
    size = 10,
    search = '',
): Promise<IncidentPage> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
    })

    const normalizedSearch = search.trim()

    if (normalizedSearch) {
        params.set('search', normalizedSearch)
    }

    return request<IncidentPage>(
        `/incidents?${params.toString()}`,
    )
}

export function getIncidentById(id: number): Promise<Incident> {
    return request<Incident>(`/incidents/${id}`)
}

export function createIncident(
    incident: CreateIncidentRequest,
): Promise<Incident> {
    return request<Incident>(
        '/incidents',
        {
            method: 'POST',
            body: JSON.stringify(incident),
        },
    )
}

export function transitionIncident(
    incidentId: number,
    status: IncidentStatus,
): Promise<Incident> {
    return request<Incident>(
        `/incidents/${incidentId}/transitions`,
{
    method: 'POST',
        body: JSON.stringify({ status }),
},
)
}

export function assignIncident(
    incidentId: number,
    userId: number,
): Promise<Incident> {
    return request<Incident>(
        `/incidents/${incidentId}/assignee`,
        {
            method: 'PUT',
            body: JSON.stringify({ userId }),
        },
    )
}

export function getIncidentUpdates(
    incidentId: number,
): Promise<IncidentUpdate[]> {
    return request<IncidentUpdate[]>(
        `/incidents/${incidentId}/updates`,
    )
}

export function addIncidentUpdate(
    incidentId: number,
    message: string,
): Promise<IncidentUpdate> {
    return request<IncidentUpdate>(
        `/incidents/${incidentId}/updates`,
        {
            method: 'POST',
            body: JSON.stringify({ message }),
        },
    )
}

export function getIncidentHistory(
    incidentId: number,
): Promise<IncidentHistoryItem[]> {
    return request<IncidentHistoryItem[]>(
        `/incidents/${incidentId}/history`,
    )
}

export function getDashboardSummary(): Promise<DashboardSummary> {
    return request<DashboardSummary>('/dashboard/summary')
}
