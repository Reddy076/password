import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { Router, provideRouter } from '@angular/router';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: Router;
  let authServiceSpy: jasmine.SpyObj<AuthenticationService>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    authServiceSpy = jasmine.createSpyObj('AuthenticationService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthenticationService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    httpMock = TestBed.inject(HttpTestingController);
    spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when form is invalid', () => {
    component.onSubmit();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should navigate to dashboard on successful login with token', () => {
    authServiceSpy.login.and.returnValue(of({
      accessToken: 'fake-jwt-token',
      refreshToken: 'fake-refresh',
      requires2FA: false
    }) as any);

    component.loginForm.setValue({ username: 'testuser', password: 'Test@123456' });
    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalled();
    expect(localStorage.getItem('access_token')).toBe('fake-jwt-token');
    expect(localStorage.getItem('refresh_token')).toBe('fake-refresh');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.isLoading).toBeFalse();
  });

  it('should show OTP step when requires2FA is true', () => {
    authServiceSpy.login.and.returnValue(of({
      requires2FA: true,
      username: 'testuser'
    }) as any);

    component.loginForm.setValue({ username: 'testuser', password: 'Test@123456' });
    component.onSubmit();

    expect(component.showOtpStep).toBeTrue();
    expect(component.pendingUsername).toBe('testuser');
    expect(component.successMessage).toContain('authenticator app');
  });

  it('should use environment.apiBaseUrl for OTP verify URL', () => {
    component.showOtpStep = true;
    component.pendingUsername = 'testuser';
    component.otpForm.setValue({ otpCode: '123456' });
    component.onSubmitOtp();

    const expectedUrl = `${environment.apiBaseUrl}/api/auth/verify-otp?username=testuser&code=123456`;
    const req = httpMock.expectOne(expectedUrl);
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: 'tok', refreshToken: 'ref' });
  });

  it('should verify OTP and navigate to dashboard', () => {
    component.showOtpStep = true;
    component.pendingUsername = 'testuser';

    component.otpForm.setValue({ otpCode: '123456' });
    component.onSubmitOtp();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/verify-otp?username=testuser&code=123456`);
    expect(req.request.method).toBe('POST');
    req.flush({
      accessToken: 'otp-jwt-token',
      refreshToken: 'otp-refresh',
      requires2FA: false
    });

    expect(localStorage.getItem('access_token')).toBe('otp-jwt-token');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should show error on invalid OTP', () => {
    component.showOtpStep = true;
    component.pendingUsername = 'testuser';

    component.otpForm.setValue({ otpCode: '000000' });
    component.onSubmitOtp();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/verify-otp?username=testuser&code=000000`);
    req.flush({ message: 'Invalid 2FA code' }, { status: 400, statusText: 'Bad Request' });

    expect(component.errorMessage).toBe('Invalid 2FA code');
  });

  it('should resend OTP and show success message', () => {
    component.pendingUsername = 'testuser';

    component.onResendOtp();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/auth/resend-otp?username=testuser`);
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'OTP resent' });

    expect(component.successMessage).toContain('new verification code');
  });

  it('should reset to login form on backToLogin', () => {
    component.showOtpStep = true;
    component.pendingUsername = 'testuser';
    component.errorMessage = 'old error';

    component.backToLogin();

    expect(component.showOtpStep).toBeFalse();
    expect(component.pendingUsername).toBe('');
    expect(component.errorMessage).toBe('');
  });

  it('should show error message on login failure', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({
      error: { message: 'Invalid username or password' }
    })) as any);

    component.loginForm.setValue({ username: 'testuser', password: 'wrong' });
    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid username or password');
    expect(component.isLoading).toBeFalse();
  });
});
