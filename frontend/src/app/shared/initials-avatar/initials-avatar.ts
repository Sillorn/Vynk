import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Decorative, generated initials avatar. Vynk never accepts uploads, so identity is represented by
 * the profile's initials over a deterministic colour derived from the username.
 */
@Component({
  selector: 'app-initials-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="initials-avatar"
      aria-hidden="true"
      [style.--avatar-size]="avatarSize() + 'px'"
      [style.--avatar-hue]="hue()"
    >
      <span class="initials-avatar__text">{{ initials() }}</span>
    </span>
  `,
  styleUrl: './initials-avatar.scss',
})
export class InitialsAvatar {
  readonly firstName = input<string>('');
  readonly lastName = input<string>('');
  readonly username = input<string>('');
  readonly size = input<number>(96);

  protected readonly avatarSize = computed(() => {
    const size = this.size();
    return Number.isFinite(size) && size > 0 ? Math.round(size) : 96;
  });

  protected readonly initials = computed(() => {
    const words = `${this.firstName()} ${this.lastName()}`
      .trim()
      .split(/\s+/)
      .filter((word) => word.length > 0);

    const letters = words.map((word) => firstLetter(word));
    const fromName = words.length > 1 ? `${letters[0]}${letters[letters.length - 1]}` : letters[0];

    return (fromName || firstLetter(this.username()) || '?').toLocaleUpperCase();
  });

  protected readonly hue = computed(() => {
    const seed = this.username().trim() || `${this.firstName()} ${this.lastName()}`;
    let hash = 0;
    for (const character of seed.toLocaleLowerCase()) {
      hash = (hash * 31 + (character.codePointAt(0) ?? 0)) % 360;
    }
    return hash;
  });
}

function firstLetter(value: string): string {
  return Array.from(value.trim())[0] ?? '';
}
