import { ApplicationConfig, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { BASE_PATH } from './core/api';
import { environment } from '../environments/environment';
import { routes } from './app.routes';
import { LucideAngularModule, LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut, ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye, EyeOff, X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle, Calendar } from 'lucide-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(withInterceptors([
      authInterceptor,
      errorInterceptor
    ])),
    { provide: BASE_PATH, useValue: environment.apiBaseUrl },
    importProvidersFrom(LucideAngularModule.pick({ LayoutDashboard, Lock, Star, Trash2, Folder, Settings, LogOut, ChevronLeft, ChevronRight, Vault, Search, Check, RotateCw, ArrowRight, Plus, Eye, EyeOff, X, Copy, Edit2, ShieldAlert, Key, Smartphone, Laptop, MapPin, AlertTriangle, Calendar }))
  ]
};

