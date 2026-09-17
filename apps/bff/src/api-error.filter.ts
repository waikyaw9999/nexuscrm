import { ArgumentsHost, Catch, ExceptionFilter } from '@nestjs/common';
import type { Response } from 'express';
import { ApiHttpError } from './proxy/api-client';

@Catch(ApiHttpError)
export class ApiErrorFilter implements ExceptionFilter {
  catch(exception: ApiHttpError, host: ArgumentsHost) {
    const res = host.switchToHttp().getResponse<Response>();
    res.status(exception.status).json(exception.body ?? { code: 'UPSTREAM', message: 'API error' });
  }
}
