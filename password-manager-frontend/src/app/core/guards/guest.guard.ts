import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../state/auth.state';

// Redirects authenticated users away from auth pages (login, register)
export const guestGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
