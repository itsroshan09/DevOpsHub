
export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type IncidentStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED'

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
}
export interface IncidentPage {
    content: Incident[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}
export type UserRole = 'ADMIN' | 'RESPONDER' | 'VIEWER'

export interface CurrentUser {
    id: number
    email: string
    displayName: string
    role: UserRole
    createdAt: string
}
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

const API_BASE = '/api'
const TOKEN_KEY = 'devopshub_access_token'

export function getAccessToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY)
}

export function saveAccessToken(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearAccessToken(): void {
    sessionStorage.removeItem(TOKEN_KEY)
}

async function request<T>(
    path: string,
    options: RequestInit = {},
    authenticated = true,
): Promise<T> {
    const headers = new Headers(options.headers)
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
                    : 'Invalid email or password.',
            )
        }

        if (response.status === 403) {
            throw new Error('You do not have permission to perform this action.')
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

export function login(credentials: LoginRequest): Promise<LoginResponse> {
    return request<LoginResponse>(
        '/auth/login',
        {
            method: 'POST',
            body: JSON.stringify(credentials),
        },
        false,
    )
}
export function getIncidentHistory(
    incidentId: number,
): Promise<IncidentHistoryItem[]> {
    return request<IncidentHistoryItem[]>(
        `/incidents/${incidentId}/history`,
    )
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

    return request<IncidentPage>(`/incidents?${params.toString()}`)
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
export function getCurrentUser(): Promise<CurrentUser> {
    return request<CurrentUser>('/users/me')
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


export function getIncidentById(id: number): Promise<Incident> {
    return request<Incident>(`/incidents/${id}`)
}
export function getDashboardSummary(): Promise<DashboardSummary> {
    return request<DashboardSummary>('/dashboard/summary')
}
export function createIncident(
    incident: CreateIncidentRequest,
): Promise<Incident> {
    return request<Incident>('/incidents', {
        method: 'POST',
        body: JSON.stringify(incident),
    })
}