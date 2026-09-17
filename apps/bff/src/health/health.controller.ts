import { Controller, Get } from '@nestjs/common';

@Controller('bff/health')
export class HealthController {
  @Get()
  get() {
    return { status: 'ok', service: 'nexuscrm-bff' };
  }
}
