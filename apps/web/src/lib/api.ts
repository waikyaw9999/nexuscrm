const API = '/bff';

function getCookie(name: string): string | undefined {
  const match = document.cookie.split('; ').find((row) => row.startsWith(name + '='));
  return match ? decodeURIComponent(match.split('=').slice(1).join('=')) : undefined;
}

async function ensureCsrf(): Promise<string> {
  const existing = getCookie('nexus_csrf');
  if (existing) return existing;
  const res = await fetch(`${API}/auth/csrf`, { credentials: 'include' });
  const data = await res.json();
  return data.csrfToken as string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const csrf = await ensureCsrf();
  const headers = new Headers(init.headers);
  headers.set('X-CSRF-Token', csrf);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  const res = await fetch(`${API}${path}`, { ...init, headers, credentials: 'include' });
  if (res.status === 204) return undefined as T;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new ApiError(res.status, data.code || 'ERROR', data.message || 'Request failed');
  }
  return data as T;
}

export const Auth = {
  me: () => api('/auth/me'),
  login: (email: string, password: string) => api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (body: object) => api('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => api('/auth/logout', { method: 'POST' }),
};

export const Crm = {
  dashboard: () => api('/api/v1/dashboard'),
  contacts: (q = '') => api(`/api/v1/contacts?q=${encodeURIComponent(q)}`),
  createContact: (body: object) => api('/api/v1/contacts', { method: 'POST', body: JSON.stringify(body) }),
  companies: (q = '') => api(`/api/v1/companies?q=${encodeURIComponent(q)}`),
  createCompany: (body: object) => api('/api/v1/companies', { method: 'POST', body: JSON.stringify(body) }),
  deals: (q = '') => api(`/api/v1/deals${q ? `?q=${encodeURIComponent(q)}` : ''}`),
  createDeal: (body: object) => api('/api/v1/deals', { method: 'POST', body: JSON.stringify(body) }),
  moveDeal: (id: string, stage: string) => api(`/api/v1/deals/${id}/stage`, { method: 'PATCH', body: JSON.stringify({ stage }) }),
  activities: () => api('/api/v1/activities'),
  createActivity: (body: object) => api('/api/v1/activities', { method: 'POST', body: JSON.stringify(body) }),
  completeActivity: (id: string) => api(`/api/v1/activities/${id}/complete`, { method: 'PATCH' }),
  mailboxes: () => api('/api/v1/mailboxes'),
  connectMock: () => api('/api/v1/mailboxes/mock', { method: 'POST' }),
  messages: (id: string, folder: string) => api(`/api/v1/mailboxes/${id}/messages?folder=${folder}`),
  message: (id: string, messageId: string) => api(`/api/v1/mailboxes/${id}/messages/${messageId}`),
  send: (id: string, body: object) => api(`/api/v1/mailboxes/${id}/send`, { method: 'POST', body: JSON.stringify(body) }),
  draft: (id: string, body: object) => api(`/api/v1/mailboxes/${id}/drafts`, { method: 'POST', body: JSON.stringify(body) }),
  profile: () => api('/api/v1/settings/profile'),
  tenant: () => api('/api/v1/settings/tenant'),
  team: () => api('/api/v1/settings/team'),
  updateTenant: (body: object) => api('/api/v1/settings/tenant', { method: 'PATCH', body: JSON.stringify(body) }),
  updateProfile: (body: object) => api('/api/v1/settings/profile', { method: 'PATCH', body: JSON.stringify(body) }),
  invite: (body: object) => api('/api/v1/invites', { method: 'POST', body: JSON.stringify(body) }),
  plan: () => api('/api/v1/billing/plan'),
  checkout: (plan: string) => api('/api/v1/billing/checkout', { method: 'POST', body: JSON.stringify({ plan }) }),
};

export type Me = {
  userId: string;
  email: string;
  fullName: string;
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  plan: string;
  role: string;
};
