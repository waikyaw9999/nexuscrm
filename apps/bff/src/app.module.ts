import { Module } from '@nestjs/common';
import { APP_FILTER, APP_GUARD } from '@nestjs/core';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import { AuthController } from './auth/auth.controller';
import { ProxyController } from './proxy/proxy.controller';
import { OauthController } from './oauth/oauth.controller';
import { HealthController } from './health/health.controller';
import { ApiErrorFilter } from './api-error.filter';

@Module({
  imports: [
    ThrottlerModule.forRoot({
      throttlers: [{ ttl: 60000, limit: 120 }],
    }),
  ],
  controllers: [AuthController, ProxyController, OauthController, HealthController],
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
    { provide: APP_FILTER, useClass: ApiErrorFilter },
  ],
})
export class AppModule {}
