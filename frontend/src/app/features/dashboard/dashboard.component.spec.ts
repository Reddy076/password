import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { UserControllerService } from '../../core/api/api/userController.service';
import { of } from 'rxjs';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let userSpy: jasmine.SpyObj<UserControllerService>;

  beforeEach(async () => {
    localStorage.clear();

    userSpy = jasmine.createSpyObj('UserControllerService', ['getDashboard', 'getActivityHeatmap']);
    userSpy.getDashboard.and.returnValue(of({}) as any);
    userSpy.getActivityHeatmap.and.returnValue(of({
      totalAccesses: 10,
      peakDay: 'Monday',
      peakHour: 14,
      accessByDay: [1, 2, 3, 4, 0, 0, 0]
    }) as any);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserControllerService, useValue: userSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default username to "User" when no token', () => {
    expect(component.username).toBe('User');
  });

  it('should extract username from JWT on init', () => {
    const payload = btoa(JSON.stringify({ sub: 'jane_doe' }));
    const fakeToken = `header.${payload}.signature`;
    localStorage.setItem('access_token', fakeToken);

    component.ngOnInit();

    expect(component.username).toBe('jane_doe');
  });
});

