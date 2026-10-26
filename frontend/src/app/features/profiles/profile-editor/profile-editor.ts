import { Component, HostListener, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProfileService } from '../../../core/profiles/profile.service';
import { PublicProfile } from '../../../core/profiles/profile.models';

@Component({
  selector: 'app-profile-editor',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <main class="page editor">
      <h1>Edit your profile</h1>
      @if (loading()) {
        <p role="status">Loading profile…</p>
      } @else {
        @if (message()) {
          <p role="status">{{ message() }}</p>
        }
        <form [formGroup]="form" (ngSubmit)="save()">
          <fieldset [disabled]="saving()">
            <legend>Basic information</legend>
            <label>First name<input formControlName="firstName" maxlength="100" /></label>
            <label>Last name<input formControlName="lastName" maxlength="100" /></label>
            <label>Headline<input formControlName="headline" maxlength="200" /></label>
            <label>Summary<textarea formControlName="summary" maxlength="2000"></textarea></label>
            <label>Location<input formControlName="location" maxlength="200" /></label>
          </fieldset>
          @for (section of sections; track section.key) {
            <fieldset>
              <legend>{{ section.label }}</legend>
              <div [formArrayName]="section.key">
                @for (row of array(section.key).controls; track row; let i = $index) {
                  <div class="row" [formGroupName]="i">
                    @for (field of section.fields; track field.key) {
                      <label>
                        {{ field.label }}
                        <input [type]="fieldType(field)" [formControlName]="field.key" />
                      </label>
                    }
                    <button type="button" (click)="remove(section.key, i)" [disabled]="saving()">
                      Remove
                    </button>
                    <button
                      type="button"
                      (click)="move(section.key, i, -1)"
                      [disabled]="i === 0 || saving()"
                    >
                      Move up
                    </button>
                    <button
                      type="button"
                      (click)="move(section.key, i, 1)"
                      [disabled]="i === array(section.key).length - 1 || saving()"
                    >
                      Move down
                    </button>
                  </div>
                }
              </div>
              <button type="button" (click)="add(section.key)" [disabled]="saving()">
                Add {{ section.item }}
              </button>
            </fieldset>
          }
          <button type="submit" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Saving…' : 'Save all changes' }}
          </button>
          <button type="button" (click)="cancel()" [disabled]="saving()">Cancel changes</button>
        </form>
      }
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .editor {
        max-width: 850px;
        margin: 2rem auto;
        padding: 1.5rem;
      }
      form,
      fieldset {
        display: grid;
        gap: 1rem;
      }
      fieldset {
        border: 1px solid #bbb;
        border-radius: 0.5rem;
        padding: 1rem;
      }
      .row {
        display: flex;
        align-items: end;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      label {
        display: grid;
        gap: 0.3rem;
      }
      input,
      textarea {
        font: inherit;
        padding: 0.6rem;
        border: 1px solid #777;
        border-radius: 0.3rem;
      }
    `,
  ],
})
export class ProfileEditor {
  private fb = inject(FormBuilder).nonNullable;
  private api = inject(ProfileService);
  private original!: PublicProfile;
  loading = signal(true);
  saving = signal(false);
  message = signal('');
  sections = [
    {
      key: 'links',
      label: 'Links',
      item: 'link',
      fields: [
        { key: 'label', label: 'Label' },
        { key: 'url', label: 'URL' },
      ],
    },
    {
      key: 'experience',
      label: 'Experience',
      item: 'role',
      fields: [
        { key: 'title', label: 'Title' },
        { key: 'company', label: 'Company' },
        { key: 'location', label: 'Location' },
        { key: 'startDate', label: 'Start date', type: 'date' },
        { key: 'endDate', label: 'End date', type: 'date' },
        { key: 'current', label: 'I currently work here', type: 'checkbox' },
        { key: 'description', label: 'Description' },
      ],
    },
    {
      key: 'education',
      label: 'Education',
      item: 'education entry',
      fields: [
        { key: 'school', label: 'School' },
        { key: 'degree', label: 'Degree' },
        { key: 'fieldOfStudy', label: 'Field of study' },
        { key: 'startDate', label: 'Start date', type: 'date' },
        { key: 'endDate', label: 'End date', type: 'date' },
        { key: 'description', label: 'Description' },
      ],
    },
    {
      key: 'certifications',
      label: 'Certifications',
      item: 'certification',
      fields: [
        { key: 'name', label: 'Name' },
        { key: 'issuer', label: 'Issuer' },
        { key: 'issueDate', label: 'Issue date', type: 'date' },
        { key: 'expirationDate', label: 'Expiration date', type: 'date' },
        { key: 'credentialUrl', label: 'Credential URL' },
      ],
    },
    {
      key: 'projects',
      label: 'Projects',
      item: 'project',
      fields: [
        { key: 'name', label: 'Name' },
        { key: 'description', label: 'Description' },
        { key: 'url', label: 'URL' },
        { key: 'startDate', label: 'Start date', type: 'date' },
        { key: 'endDate', label: 'End date', type: 'date' },
      ],
    },
    { key: 'skills', label: 'Skills', item: 'skill', fields: [{ key: 'name', label: 'Name' }] },
    {
      key: 'languages',
      label: 'Languages',
      item: 'language',
      fields: [
        { key: 'name', label: 'Name' },
        { key: 'proficiency', label: 'Proficiency' },
      ],
    },
  ] as const;
  form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    headline: ['', [Validators.required, Validators.maxLength(200)]],
    summary: ['', [Validators.required, Validators.maxLength(2000)]],
    location: [''],
    links: this.fb.array([]),
    experience: this.fb.array([]),
    education: this.fb.array([]),
    certifications: this.fb.array([]),
    projects: this.fb.array([]),
    skills: this.fb.array([]),
    languages: this.fb.array([]),
  });
  constructor() {
    this.api.getOwnProfile().subscribe({
      next: (p) => {
        this.original = p;
        this.form.patchValue({
          firstName: p.firstName,
          lastName: p.lastName,
          headline: p.headline,
          summary: p.summary,
          location: p.location ?? '',
        });
        for (const s of this.sections)
          for (const item of p[s.key] ?? [])
            this.array(s.key).push(
              this.fb.group(
                Object.fromEntries(s.fields.map((f) => [f.key, [(item as any)[f.key] ?? '']])),
              ),
            );
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Unable to load profile. Please sign in and try again.');
        this.loading.set(false);
      },
    });
  }
  fieldType(field: { type?: string; key: string; label: string }): string {
    return field.type ?? 'text';
  }
  array(key: string): FormArray {
    return this.form.controls[key as keyof typeof this.form.controls] as FormArray;
  }
  add(key: string) {
    const section = this.sections.find((x) => x.key === key)!;
    this.array(key).push(
      this.fb.group(Object.fromEntries(section.fields.map((f) => [f.key, ['']]))),
    );
  }
  remove(key: string, i: number) {
    this.array(key).removeAt(i);
  }
  move(key: string, i: number, d: number) {
    const a = this.array(key),
      row = a.at(i);
    a.removeAt(i);
    a.insert(i + d, row);
  }
  private hasSafeLinks(value: Record<string, any>): boolean {
    for (const key of ['links', 'projects', 'certifications'])
      for (const item of value[key] ?? []) {
        const url = item.url ?? item.credentialUrl;
        if (!url) continue;
        try {
          const parsed = new URL(url);
          if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return false;
        } catch {
          return false;
        }
      }
    return true;
  }
  @HostListener('window:beforeunload', ['$event']) unload(e: BeforeUnloadEvent) {
    if (this.form.dirty) {
      e.preventDefault();
      e.returnValue = true;
    }
  }
  cancel() {
    if (this.form.dirty && !confirm('Discard unsaved changes?')) return;
    this.form.reset({
      firstName: this.original.firstName,
      lastName: this.original.lastName,
      headline: this.original.headline,
      summary: this.original.summary,
      location: this.original.location ?? '',
    });
    for (const s of this.sections) this.array(s.key).clear();
    for (const s of this.sections)
      for (const item of this.original[s.key] ?? [])
        this.array(s.key).push(
          this.fb.group(
            Object.fromEntries(s.fields.map((f) => [f.key, [(item as any)[f.key] ?? '']])),
          ),
        );
    this.form.markAsPristine();
  }
  save() {
    if (this.form.invalid || this.saving()) return;
    const v = this.form.getRawValue();
    if (!this.hasSafeLinks(v)) {
      this.message.set('Links must use a valid http:// or https:// address.');
      return;
    }
    this.saving.set(true);
    this.message.set('');
    this.api.saveAll(v).subscribe({
      next: (p) => {
        this.original = p;
        this.form.markAsPristine();
        this.saving.set(false);
        this.message.set('Profile saved.');
      },
      error: () => {
        this.saving.set(false);
        this.message.set('Save failed. Check your entries and try again.');
      },
    });
  }
}
