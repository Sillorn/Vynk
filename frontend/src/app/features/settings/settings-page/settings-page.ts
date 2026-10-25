import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-settings-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(12)]],
  });

  protected readonly pwError = signal<string | null>(null);
  protected readonly pwSuccess = signal(false);
  protected readonly pwSubmitting = signal(false);

  protected readonly delSubmitting = signal(false);
  protected readonly delError = signal<string | null>(null);

  changePassword(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.pwSubmitting.set(true);
    this.pwError.set(null);
    this.pwSuccess.set(false);
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.http
      .post('/api/v1/auth/password', { currentPassword, newPassword }, { withCredentials: true })
      .subscribe({
        next: () => {
          this.pwSubmitting.set(false);
          this.pwSuccess.set(true);
          this.form.reset();
        },
        error: (err) => {
          this.pwSubmitting.set(false);
          this.pwError.set(err?.error?.detail || 'Password update failed.');
        },
      });
  }

  deleteAccount(): void {
    if (!confirm('Delete your account? This action cannot be undone.')) {
      return;
    }
    this.delSubmitting.set(true);
    this.delError.set(null);
    this.http.delete('/api/v1/auth/account', { withCredentials: true }).subscribe({
      next: () => {
        this.delSubmitting.set(false);
        this.auth.user.set(null);
        this.router.navigateByUrl('/login');
      },
      error: (err) => {
        this.delSubmitting.set(false);
        this.delError.set(err?.error?.detail || 'Account deletion failed.');
      },
    });
  }
}
