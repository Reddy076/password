import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { UserControllerService } from '../../../core/api/api/userController.service';
import { UserSettingsControllerService } from '../../../core/api/api/userSettingsController.service';
import { TwoFactorAuthControllerService } from '../../../core/api/api/twoFactorAuthController.service';
import { SessionControllerService } from '../../../core/api/api/sessionController.service';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userSpy: jasmine.SpyObj<UserControllerService>;
  let settingsSpy: jasmine.SpyObj<UserSettingsControllerService>;
  let tfaSpy: jasmine.SpyObj<TwoFactorAuthControllerService>;
  let sessionSpy: jasmine.SpyObj<SessionControllerService>;

  beforeEach(async () => {
    userSpy = jasmine.createSpyObj('UserControllerService', [
      'getProfile', 'changePassword', 'deleteAccount', 'cancelDeletion'
    ]);
    settingsSpy = jasmine.createSpyObj('UserSettingsControllerService', ['getSettings', 'updateSettings']);
    tfaSpy = jasmine.createSpyObj('TwoFactorAuthControllerService', [
      'getStatus', 'setup2FA', 'verifySetup', 'disable2FA', 'getBackupCodes', 'regenerateCodes'
    ]);
    sessionSpy = jasmine.createSpyObj('SessionControllerService', [
      'getActiveSessions', 'getCurrentSession', 'terminateSession', 'terminateAllSessions'
    ]);

    userSpy.getProfile.and.returnValue(of({ username: 'testuser', email: 'test@test.com' }) as any);
    settingsSpy.getSettings.and.returnValue(of({ readOnlyMode: false }) as any);
    tfaSpy.getStatus.and.returnValue(of({ enabled: false }) as any);

    sessionSpy.getActiveSessions.and.returnValue(of([
      { id: 1, deviceInfo: 'Windows PC', lastAccessedAt: '2023-10-01T10:00:00Z' },
      { id: 2, deviceInfo: 'Mobile iOS', lastAccessedAt: '2023-10-02T12:00:00Z' }
    ]) as any);
    sessionSpy.getCurrentSession.and.returnValue(of({ id: 2, deviceInfo: 'Mobile iOS' }) as any);

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        provideRouter([]),
        { provide: UserControllerService, useValue: userSpy },
        { provide: UserSettingsControllerService, useValue: settingsSpy },
        { provide: TwoFactorAuthControllerService, useValue: tfaSpy },
        { provide: SessionControllerService, useValue: sessionSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;

    // Mock window.confirm
    spyOn(window, 'confirm').and.returnValue(true);

    localStorage.setItem('access_token', 'mock_token');

    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // --- Session Management Tests ---

  it('should load sessions on init and sort them by date descending', () => {
    expect(sessionSpy.getActiveSessions).toHaveBeenCalled();
    expect(sessionSpy.getCurrentSession).toHaveBeenCalledWith('Bearer mock_token');

    expect(component.isLoadingSessions).toBeFalse();
    expect(component.currentSessionId).toBe(2);
    expect(component.activeSessions.length).toBe(2);
    // iOS should be first (id=2) because 10-02 > 10-01
    expect(component.activeSessions[0].id).toBe(2);
    expect(component.activeSessions[1].id).toBe(1);
  });

  it('should handle error when loading sessions', () => {
    sessionSpy.getActiveSessions.and.returnValue(throwError(() => new Error('Error')));
    component.loadSessions();
    expect(component.isLoadingSessions).toBeFalse();
    expect(component.errorMessage).toBe('Failed to load active sessions.');
  });

  it('should revoke a specific session', () => {
    sessionSpy.terminateSession.and.returnValue(of({ message: 'Terminated' }) as any);

    component.onRevokeSession(1);

    expect(window.confirm).toHaveBeenCalled();
    expect(sessionSpy.terminateSession).toHaveBeenCalledWith(1);
    expect(component.successMessage).toBe('Device logged out successfully.');
    expect(sessionSpy.getActiveSessions).toHaveBeenCalledTimes(2); // once on init, once here
  });

  it('should handle error when revoking a session', () => {
    sessionSpy.terminateSession.and.returnValue(throwError(() => ({ error: { message: 'Cannot terminate' } })));

    component.onRevokeSession(1);

    expect(component.errorMessage).toBe('Cannot terminate');
  });

  it('should revoke all other sessions', () => {
    sessionSpy.terminateAllSessions.and.returnValue(of({ message: 'All Terminated' }) as any);

    component.onRevokeAllOtherSessions();

    expect(window.confirm).toHaveBeenCalled();
    expect(sessionSpy.terminateAllSessions).toHaveBeenCalled();
    expect(component.successMessage).toBe('All other devices logged out successfully.');
  });

  // --- Profile Tests ---

  it('should load profile on init', () => {
    expect(component.userProfile?.username).toBe('testuser');
    expect(component.isLoadingProfile).toBeFalse();
  });

  it('should load 2FA status on init', () => {
    expect(component.is2FAEnabled).toBeFalse();
    expect(component.isLoading2FA).toBeFalse();
  });

  it('should start 2FA setup and show QR code', () => {
    tfaSpy.setup2FA.and.returnValue(of({
      qrCodeUrl: 'https://example.com/qr.png',
      secretKey: 'ABCD1234'
    }) as any);

    component.onStartSetup();

    expect(component.showSetupStep).toBeTrue();
    expect(component.showVerifyStep).toBeTrue();
    expect(component.qrCodeUrl).toBe('https://example.com/qr.png');
    expect(component.secretKey).toBe('ABCD1234');
  });

  it('should verify setup and enable 2FA', () => {
    tfaSpy.verifySetup.and.returnValue(of({
      success: true,
      message: '2FA enabled',
      backupCodes: ['CODE1', 'CODE2', 'CODE3']
    }) as any);

    component.setupCodeForm.setValue({ verificationCode: '123456' });
    component.onVerifySetup();

    expect(component.is2FAEnabled).toBeTrue();
    expect(component.backupCodes.length).toBe(3);
    expect(component.showBackupCodes).toBeTrue();
  });

  it('should disable 2FA', () => {
    component.is2FAEnabled = true;
    tfaSpy.disable2FA.and.returnValue(of({ message: 'Disabled' }) as any);

    component.onDisable2FA();

    expect(component.is2FAEnabled).toBeFalse();
  });

  it('should view backup codes', () => {
    tfaSpy.getBackupCodes.and.returnValue(of(['A1', 'B2', 'C3']) as any);

    component.onViewBackupCodes();

    expect(component.backupCodes).toEqual(['A1', 'B2', 'C3']);
    expect(component.showBackupCodes).toBeTrue();
  });

  it('should regenerate backup codes', () => {
    tfaSpy.regenerateCodes.and.returnValue(of({
      success: true,
      backupCodes: ['NEW1', 'NEW2']
    }) as any);

    component.onRegenerateCodes();

    expect(component.backupCodes).toEqual(['NEW1', 'NEW2']);
    expect(component.showBackupCodes).toBeTrue();
  });

  it('should cancel setup', () => {
    component.showSetupStep = true;
    component.showVerifyStep = true;

    component.onCancelSetup();

    expect(component.showSetupStep).toBeFalse();
    expect(component.showVerifyStep).toBeFalse();
  });

  it('should hide backup codes', () => {
    component.showBackupCodes = true;
    component.onHideBackupCodes();
    expect(component.showBackupCodes).toBeFalse();
  });

  // --- Account Deletion Tests ---

  it('should open delete modal when onDeleteAccount is called', () => {
    component.onDeleteAccount();

    expect(component.showDeleteModal).toBeTrue();
  });

  it('should close delete modal when onCancelDeleteModal is called', () => {
    component.showDeleteModal = true;
    component.onCancelDeleteModal();

    expect(component.showDeleteModal).toBeFalse();
  });

  it('should not submit delete when form is invalid', () => {
    component.deleteForm.setValue({ masterPassword: '', confirmation: false });
    component.onConfirmDelete();

    expect(userSpy.deleteAccount).not.toHaveBeenCalled();
  });

  it('should call deleteAccount API and show success message on confirm', () => {
    userSpy.deleteAccount.and.returnValue(of({
      message: 'Account scheduled for deletion in 30 days.'
    }) as any);
    userSpy.getProfile.and.returnValue(of({
      username: 'testuser',
      email: 'test@test.com',
      deletionScheduledAt: '2026-03-26T00:00:00'
    }) as any);

    component.deleteForm.setValue({ masterPassword: 'MyP@ssword1', confirmation: true });
    component.onConfirmDelete();

    expect(userSpy.deleteAccount).toHaveBeenCalledWith({
      masterPassword: 'MyP@ssword1',
      confirmation: true
    });
    expect(component.isDeletingAccount).toBeFalse();
    expect(component.showDeleteModal).toBeFalse();
    expect(component.successMessage).toContain('scheduled for deletion');
  });

  it('should show error message when deleteAccount fails', () => {
    userSpy.deleteAccount.and.returnValue(throwError(() => ({
      error: { message: 'Invalid master password' }
    })));

    component.deleteForm.setValue({ masterPassword: 'wrong', confirmation: true });
    component.onConfirmDelete();

    expect(component.isDeletingAccount).toBeFalse();
    expect(component.errorMessage).toBe('Invalid master password');
  });

  it('should call cancelDeletion API and refresh profile', () => {
    userSpy.cancelDeletion.and.returnValue(of({
      message: 'Account deletion cancelled.'
    }) as any);
    userSpy.getProfile.and.returnValue(of({
      username: 'testuser',
      email: 'test@test.com'
    }) as any);

    component.onCancelDeletion();

    expect(userSpy.cancelDeletion).toHaveBeenCalled();
    expect(component.isCancellingDeletion).toBeFalse();
  });

  it('should detect deletion pending status from profile response', () => {
    userSpy.getProfile.and.returnValue(of({
      username: 'testuser',
      email: 'test@test.com',
      deletionScheduledAt: '2026-03-26T00:00:00'
    }) as any);

    component.loadProfile();

    expect(component.isDeletionPending).toBeTrue();
    expect(component.deletionDate).toBeTruthy();
  });

  it('should show error when cancelDeletion fails', () => {
    userSpy.cancelDeletion.and.returnValue(throwError(() => ({
      error: { message: 'No deletion scheduled' }
    })));

    component.onCancelDeletion();

    expect(component.isCancellingDeletion).toBeFalse();
    expect(component.errorMessage).toBe('No deletion scheduled');
  });
});
