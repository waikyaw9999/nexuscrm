export class ApiHttpError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: unknown,
  ) {
    super('API error');
  }
}

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080';
const BFF_KEY = process.env.NEXUS_BFF_API_KEY || 'dev-bff-key';

export async function apiFetch(
  path: string,
  opts: { method: string; body?: unknown; accessToken?: string; headers?: Record<string, string> } = {
    method: 'GET',
  },
): Promise<any> {
  const headers: Record<string, string> = {
    'X-BFF-Key': BFF_KEY,
    ...(opts.headers || {}),
  };
  if (opts.accessToken) {
    headers.Authorization = `Bearer ${opts.accessToken}`;
  }
  let body: string | undefined;
  if (opts.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(opts.body);
  }
  const res = await fetch(`${API_BASE}${path}`, { method: opts.method, headers, body });
  if (res.status === 204) {
    return null;
  }
  const text = await res.text();
  const json = text ? JSON.parse(text) : null;
  if (!res.ok) {
    throw new ApiHttpError(res.status, json);
  }
  return json;
}
