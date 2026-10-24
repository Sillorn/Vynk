import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-not-found-page',
  imports: [MatButtonModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-intro">
        <p class="eyebrow">404</p>
        <h1>This page does not exist</h1>
        <p class="lede">
          The address may be mistyped, or the page may have moved. Start again from your feed.
        </p>
      </div>

      <a matButton="filled" routerLink="/feed">Go to the feed</a>
    </section>
  `,
})
export class NotFoundPage {}
