import { createHash, randomBytes } from 'crypto';

export const CSRF_COOKIE = 'nexus_csrf';
export const ACCESS_COOKIE = 'nexus_at';
export const REFRESH_COOKIE = 'nexus_rt';

export function createCsrfToken(): string {
  return randomBytes(32).toString('hex');
}

export function hashToken(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}

export function cookieBase() {
  const secure = process.env.COOKIE_SECURE === 'true';
  const domain = process.env.COOKIE_DOMAIN || undefined;
  return {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure,
    domain,
    path: '/',
  };
}
