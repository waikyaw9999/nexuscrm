import {
  Body,
  Controller,
  Get,
  HttpCode,
  Post,
  Req,
  Res,
  UnauthorizedException,
} from '@nestjs/common';
import { Throttle } from '@nestjs/throttler';
import type { Request, Response } from 'express';
import { apiFetch, ApiHttpError } from '../proxy/api-client';
import { ACCESS_COOKIE, CSRF_COOKIE, REFRESH_COOKIE, cookieBase, createCsrfToken } from '../csrf/csrf.util';

@Controller('bff/auth')
export class AuthController {
  @Get('csrf')
  csrf(@Res({ passthrough: true }) res: Response) {
    const token = createCsrfToken();
    res.cookie(CSRF_COOKIE, token, { ...cookieBase(), httpOnly: false });
    return { csrfToken: token };
  }

  @Post('register')
  async register(@Body() body: unknown, @Res({ passthrough: true }) res: Response) {
    const data = await apiFetch('/api/v1/auth/register', { method: 'POST', body });
    this.setSession(res, data);
    return { user: data.user };
  }

  @Post('login')
  @HttpCode(200)
  @Throttle({ default: { limit: 10, ttl: 60000 } })
  async login(@Body() body: unknown, @Res({ passthrough: true }) res: Response) {
    try {
      const data = await apiFetch('/api/v1/auth/login', { method: 'POST', body });
      this.setSession(res, data);
      return { user: data.user };
    } catch (err) {
      if (err instanceof ApiHttpError) {
        throw err;
      }
      throw err;
    }
  }

  @Post('refresh')
  @HttpCode(200)
  async refresh(@Req() req: Request, @Res({ passthrough: true }) res: Response) {
    const refreshToken = req.cookies?.[REFRESH_COOKIE];
    if (!refreshToken) {
      throw new UnauthorizedException({ code: 'UNAUTHORIZED', message: 'No refresh token' });
    }
    const data = await apiFetch('/api/v1/auth/refresh', { method: 'POST', body: { refreshToken } });
    this.setSession(res, data);
    return { user: data.user };
  }

  @Post('logout')
  @HttpCode(204)
  async logout(@Req() req: Request, @Res({ passthrough: true }) res: Response) {
    const refreshToken = req.cookies?.[REFRESH_COOKIE];
    if (refreshToken) {
      try {
        await apiFetch('/api/v1/auth/logout', { method: 'POST', body: { refreshToken } });
      } catch {
        // ignore
      }
    }
    res.clearCookie(ACCESS_COOKIE, { path: '/' });
    res.clearCookie(REFRESH_COOKIE, { path: '/' });
  }

  @Get('me')
  async me(@Req() req: Request) {
    const access = req.cookies?.[ACCESS_COOKIE];
    if (!access) {
      throw new UnauthorizedException({ code: 'UNAUTHORIZED', message: 'Not signed in' });
    }
    return apiFetch('/api/v1/auth/me', { method: 'GET', accessToken: access });
  }

  private setSession(res: Response, data: { accessToken: string; refreshToken: string }) {
    const csrf = createCsrfToken();
    res.cookie(ACCESS_COOKIE, data.accessToken, { ...cookieBase(), maxAge: 15 * 60 * 1000 });
    res.cookie(REFRESH_COOKIE, data.refreshToken, { ...cookieBase(), maxAge: 14 * 24 * 60 * 60 * 1000 });
    res.cookie(CSRF_COOKIE, csrf, { ...cookieBase(), httpOnly: false, maxAge: 14 * 24 * 60 * 60 * 1000 });
  }
}
