import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DisplayNameService } from './display-name.service';
import { ProfileService } from './profile.service';

describe('DisplayNameService', () => {
  const profiles = { getPublicProfile: vi.fn() };
  let service: DisplayNameService;

  beforeEach(() => {
    profiles.getPublicProfile.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: ProfileService, useValue: profiles }],
    });
    service = TestBed.inject(DisplayNameService);
  });

  it('falls back to the username until loaded', () => {
    profiles.getPublicProfile.mockReturnValue(of({ firstName: 'Ada', lastName: 'Lovelace' }));
    expect(service.name('ada')).toBe('ada');
    service.load('ada');
    expect(service.name('ada')).toBe('Ada Lovelace');
  });

  it('requests each username only once', () => {
    profiles.getPublicProfile.mockReturnValue(of({ firstName: 'Ada', lastName: '' }));
    service.load('ada');
    service.load('ada');
    expect(profiles.getPublicProfile).toHaveBeenCalledTimes(1);
    expect(service.name('ada')).toBe('Ada');
  });

  it('allows a retry after a failure and ignores empty usernames', () => {
    profiles.getPublicProfile.mockReturnValueOnce(throwError(() => new Error('x')));
    service.load('ada');
    expect(service.name('ada')).toBe('ada');
    profiles.getPublicProfile.mockReturnValueOnce(of({ firstName: 'Ada', lastName: 'L' }));
    service.load('ada');
    expect(service.name('ada')).toBe('Ada L');
    service.load('');
    expect(profiles.getPublicProfile).toHaveBeenCalledTimes(2);
  });
});
