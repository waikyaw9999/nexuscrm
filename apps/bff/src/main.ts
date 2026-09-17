import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import cookieParser from 'cookie-parser';
import helmet from 'helmet';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { cors: false });
  app.use(helmet({ contentSecurityPolicy: false }));
  app.use(cookieParser());
  const origin = process.env.WEB_ORIGIN || 'http://localhost:5173';
  app.enableCors({
    origin: [origin, 'http://localhost:5173', 'http://localhost:8088'],
    credentials: true,
  });
  const port = Number(process.env.PORT || 3001);
  await app.listen(port, '0.0.0.0');
}

bootstrap();
