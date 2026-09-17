import {
  All,
  ArgumentsHost,
  Catch,
  Controller,
  ExceptionFilter,
  ForbiddenException,
  Req,
  Res,
  UnauthorizedException,
  UseFilters,
} from '@nestjs/common';
import type { Request, Response } from 'express';
import { ACCESS_COOKIE, CSRF_COOKIE, REFRESH_COOKIE } from '../csrf/csrf.util';
import { ApiHttpError, apiFetch } from './api-client';

@Catch(ApiHttpError)
class ApiErrorFilter implements ExceptionFilter {
  catch(exception: ApiHttpError, host: ArgumentsHost) {
    const res = host.switchToHttp().getResponse<Response>();
    res.status(exception.status).json(exception.body ?? { code: 'UPSTREAM', message: 'API error' });
  }
}

@Controller('bff/api')
@UseFilters(ApiErrorFilter)
export class ProxyController {
  @All('*')
  async proxy(@Req() req: Request, @Res() res: Response) {
    if (req.method !== 'GET' && req.method !== 'HEAD' && req.method !== 'OPTIONS') {
      const cookie = req.cookies?.[CSRF_COOKIE];
      const header = String(req.headers['x-csrf-token'] || '');
      if (!cookie || !header || cookie !== header) {
        throw new ForbiddenException({ code: 'CSRF', message: 'Invalid CSRF token' });
      }
    }

    let access = req.cookies?.[ACCESS_COOKIE] as string | undefined;
    const refresh = req.cookies?.[REFRESH_COOKIE] as string | undefined;
    if (!access && refresh) {
      try {
        const data = await apiFetch('/api/v1/auth/refresh', { method: 'POST', body: { refreshToken: refresh } });
        access = data.accessToken;
        res.cookie(ACCESS_COOKIE, data.accessToken, {
          httpOnly: true,
          sameSite: 'lax',
          secure: process.env.COOKIE_SECURE === 'true',
          path: '/',
          maxAge: 15 * 60 * 1000,
        });
        res.cookie(REFRESH_COOKIE, data.refreshToken, {
          httpOnly: true,
          sameSite: 'lax',
          secure: process.env.COOKIE_SECURE === 'true',
          path: '/',
          maxAge: 14 * 24 * 60 * 60 * 1000,
        });
      } catch {
        throw new UnauthorizedException({ code: 'UNAUTHORIZED', message: 'Session expired' });
      }
    }
    if (!access) {
      throw new UnauthorizedException({ code: 'UNAUTHORIZED', message: 'Not signed in' });
    }

    const suffix = req.url.replace(/^\/bff\/api/, '') || '/';
    const path = suffix.startsWith('/v1') ? `/api${suffix}` : `/api/v1${suffix}`;
    const rawQuery = req.originalUrl.includes('?') ? req.originalUrl.slice(req.originalUrl.indexOf('?')) : '';
    const hasBody = !['GET', 'HEAD'].includes(req.method);
    const result = await apiFetch(path + rawQuery, {
      method: req.method,
      accessToken: access,
      body: hasBody ? req.body : undefined,
    });
    if (result === null) {
      return res.status(204).send();
    }
    return res.json(result);
  }
}
