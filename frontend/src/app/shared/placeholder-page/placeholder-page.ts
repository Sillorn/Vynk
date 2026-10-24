import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-placeholder-page',
  template: `
    <section class="page">
      <div class="page-intro">
        <p class="eyebrow">Pro Network</p>
        <h1>{{ heading() }}</h1>
        <p class="lede">{{ description() }}</p>
      </div>

      <div class="surface-card placeholder-panel">
        <h2>This area is on its way</h2>
        <p>
          The application shell, navigation and design system are in place. Feature work for this
          section arrives in a later milestone.
        </p>
      </div>
    </section>
  `,
  styleUrl: './placeholder-page.scss',
})
export class PlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  protected readonly heading = toSignal(
    this.route.data.pipe(map((data) => (data['heading'] as string | undefined) ?? 'Pro Network')),
    { initialValue: 'Pro Network' },
  );

  protected readonly description = toSignal(
    this.route.data.pipe(map((data) => (data['description'] as string | undefined) ?? '')),
    { initialValue: '' },
  );
}
