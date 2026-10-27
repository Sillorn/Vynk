import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { usernameValidator } from '../../../shared/validators/username.validator';

@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, usernameValidator()]],
    password: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', [Validators.required]],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    headline: ['', [Validators.required, Validators.maxLength(200)]],
    summary: ['', [Validators.required, Validators.maxLength(2000)]],
  });

  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { confirmPassword, ...registration } = this.form.getRawValue();
    if (registration.password !== confirmPassword) {
      this.form.controls.confirmPassword.setErrors({ mismatch: true });
      this.form.controls.confirmPassword.markAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.auth.register(registration).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigateByUrl('/feed');
      },
      error: (err) => {
        this.submitting.set(false);
        const detail = err?.error?.detail || 'Registration failed.';
        this.error.set(detail);
      },
    });
  }
}
