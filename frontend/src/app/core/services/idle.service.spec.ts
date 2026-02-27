import { TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { IdleService } from './idle.service';
import { Router } from '@angular/router';
import { AuthenticationService } from '../api';
import { of, throwError } from 'rxjs';
import { NgZone } from '@angular/core';

describe('IdleService', () => {
  let service: IdleService;
  let routerSpy: jasmine.SpyObj<Router>;
  let authSpy: jasmine.SpyObj<AuthenticationService>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    authSpy = jasmine.createSpyObj('AuthenticationService', ['logout']);

    TestBed.configureTestingModule({
      providers: [
        IdleService,
        { provide: Router, useValue: routerSpy },
        { provide: AuthenticationService, useValue: authSpy }
      ]
    });

    service = TestBed.inject(IdleService);
    localStorage.clear();

    spyOn<any>(service, 'reloadPage');
  });

  afterEach(() => {
    service.stopWatching();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should timeout and logout after 15 minutes of inactivity', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(of({}) as any);

    service.startWatching();

    // Fast forward almost 15 minutes
    tick((15 * 60 * 1000) - 1000);
    expect(authSpy.logout).not.toHaveBeenCalled();

    // Fast forward the last second
    tick(1000);

    expect(authSpy.logout).toHaveBeenCalledWith('Bearer test-token');
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);

    // Flush the reload timeout
    tick(100);
    expect((service as any).reloadPage).toHaveBeenCalled();

    flush();
  }));

  it('should reset timer on user activity', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(of({}) as any);

    service.startWatching();

    // Fast forward 10 minutes
    tick(10 * 60 * 1000);

    // Simulate mouse move
    document.dispatchEvent(new Event('mousemove'));

    // Fast forward another 10 minutes (total 20 mins, but only 10 mins since activity)
    tick(10 * 60 * 1000);

    expect(authSpy.logout).not.toHaveBeenCalled();

    // Fast forward another 5 minutes to reach the 15 min mark since activity
    tick(5 * 60 * 1000);

    expect(authSpy.logout).toHaveBeenCalled();

    flush();
  }));

  it('should handle backend logout error gracefully', fakeAsync(() => {
    localStorage.setItem('access_token', 'test-token');
    authSpy.logout.and.returnValue(throwError(() => new Error('API Error')));

    service.startWatching();

    tick(15 * 60 * 1000);

    expect(authSpy.logout).toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBeNull(); // Still clears locally
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);

    flush();
  }));
});
