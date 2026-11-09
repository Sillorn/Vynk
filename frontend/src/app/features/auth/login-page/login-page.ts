import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { usernameValidator } from '../../../shared/validators/username.validator';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, usernameValidator()]],
    password: ['', [Validators.required]],
  });

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.submitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        // Only follow same-app relative paths to avoid open redirects.
        const safe = returnUrl && /^\/(?!\/)/.test(returnUrl) ? returnUrl : '/feed';
        this.router.navigateByUrl(safe);
      },
      error: (err) => {
        this.submitting.set(false);
        const detail = err?.error?.detail || 'Sign in failed.';
        this.error.set(detail);
      },
    });
  }
}
