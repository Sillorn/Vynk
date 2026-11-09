import { unsavedChangesGuard } from './unsaved-changes.guard';

describe('unsavedChangesGuard', () => {
  const run = (dirty: boolean) =>
    unsavedChangesGuard({ hasUnsavedChanges: () => dirty }, {} as never, {} as never, {} as never);

  afterEach(() => vi.restoreAllMocks());

  it('allows navigation without prompting when clean', () => {
    const confirmSpy = vi.spyOn(window, 'confirm');
    expect(run(false)).toBe(true);
    expect(confirmSpy).not.toHaveBeenCalled();
  });

  it('follows the user choice when dirty', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValueOnce(false);
    expect(run(true)).toBe(false);
    confirmSpy.mockReturnValueOnce(true);
    expect(run(true)).toBe(true);
  });
});
