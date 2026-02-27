import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthStateService } from '../state/auth.state';
import { Router } from '@angular/router';

const API_BASE = 'http://localhost:8080';

// Endpoints that do NOT require auth token
const PUBLIC_ENDPOINTS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/verify-email',
  '/api/auth/resend-verification-otp',
  '/api/auth/verify-otp',
  '/api/auth/send-otp',
  '/api/auth/resend-otp',
  '/api/auth/refresh-token',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/auth/verify-security-questions',
  '/api/auth/verify-captcha',
  '/api/auth/security-questions',
  '/api/auth/password-hint',
  '/api/auth/duress-login',
  '/api/generator/generate',
  '/api/generator/generate-multiple',
  '/api/generator/strength',
  '/api/generator/validate',
  '/api/generator/default-settings',
  '/api/health',
  '/api/shares/',
  '/api/emergency/vault/',
];

function isPublicEndpoint(url: string): boolean {
  return PUBLIC_ENDPOINTS.some(endpoint => url.includes(endpoint));
}

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  // Add base URL if relative
  let apiReq = req;
  if (!req.url.startsWith('http')) {
    apiReq = req.clone({ url: `${API_BASE}${req.url}` });
  }

  // Skip auth header for public endpoints
  if (isPublicEndpoint(apiReq.url)) {
    return next(apiReq);
  }

  // Attach Bearer token
  const token = authState.accessToken();
  if (token) {
    apiReq = apiReq.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(apiReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expired — clear auth and redirect to login
        authState.clearAuth();
        router.navigate(['/auth/login']);
      }
      return throwError(() => error);
    })
  );
};
