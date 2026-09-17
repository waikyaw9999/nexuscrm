import { Controller, Get, Query, Res } from '@nestjs/common';
import type { Response } from 'express';
import { ACCESS_COOKIE } from '../csrf/csrf.util';
import { apiFetch } from '../proxy/api-client';
import type { Request } from 'express';
import { Req } from '@nestjs/common';

@Controller('bff/oauth/gmail')
export class OauthController {
  @Get()
  start(@Res() res: Response) {
    const clientId = process.env.GOOGLE_CLIENT_ID || '';
    const redirectUri = process.env.GOOGLE_REDIRECT_URI || 'http://localhost:8088/bff/oauth/gmail/callback';
    if (!clientId) {
      return res.redirect('/app/settings?gmail=not-configured');
    }
    const scope = encodeURIComponent('https://www.googleapis.com/auth/gmail.modify email');
    const url =
      `https://accounts.google.com/o/oauth2/v2/auth?client_id=${encodeURIComponent(clientId)}` +
      `&redirect_uri=${encodeURIComponent(redirectUri)}` +
      `&response_type=code&access_type=offline&prompt=consent&scope=${scope}`;
    return res.redirect(url);
  }

  @Get('callback')
  async callback(@Query('code') code: string, @Req() req: Request, @Res() res: Response) {
    const webOrigin = process.env.WEB_ORIGIN || 'http://localhost:8088';
    if (!code) {
      return res.redirect(`${webOrigin}/app/settings?gmail=denied`);
    }
    const body = new URLSearchParams({
      code,
      client_id: process.env.GOOGLE_CLIENT_ID || '',
      client_secret: process.env.GOOGLE_CLIENT_SECRET || '',
      redirect_uri: process.env.GOOGLE_REDIRECT_URI || 'http://localhost:8088/bff/oauth/gmail/callback',
      grant_type: 'authorization_code',
    });
    const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });
    if (!tokenRes.ok) {
      return res.redirect(`${webOrigin}/app/settings?gmail=token-error`);
    }
    const tokens = (await tokenRes.json()) as {
      access_token: string;
      refresh_token?: string;
    };
    const profileRes = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
      headers: { Authorization: `Bearer ${tokens.access_token}` },
    });
    const profile = (await profileRes.json()) as { email?: string };
    const access = req.cookies?.[ACCESS_COOKIE];
    if (access && profile.email) {
      try {
        await apiFetch('/api/v1/mailboxes/gmail', {
          method: 'POST',
          accessToken: access,
          body: {
            email: profile.email,
            refreshToken: tokens.refresh_token,
            accessToken: tokens.access_token,
          },
        });
      } catch {
        return res.redirect(`${webOrigin}/app/mail?gmail=connect-error`);
      }
    }
    return res.redirect(`${webOrigin}/app/mail?gmail=connected`);
  }
}
