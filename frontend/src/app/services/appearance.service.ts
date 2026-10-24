import {computed, Injectable, signal} from '@angular/core';

interface AppearanceSettings {
  darkMode: boolean;
}

const STORAGE_KEY = 'appearance';
const DARK_MODE_CLASS = 'my-app-dark';

@Injectable({providedIn: 'root'})
export class AppearanceService {
  private readonly settings = signal<AppearanceSettings>(this.loadSettings());
  readonly darkMode = computed(() => this.settings().darkMode);

  constructor() {
    this.applyDarkMode();
  }

  toggleDarkMode(): void {
    this.save({...this.settings(), darkMode: !this.darkMode()});
    this.applyDarkMode();
  }

  private save(settings: AppearanceSettings): void {
    this.settings.set(settings);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  }

  private applyDarkMode(): void {
    document.documentElement.classList.toggle(DARK_MODE_CLASS, this.darkMode());
  }

  private loadSettings(): AppearanceSettings {
    const defaults: AppearanceSettings = {
      darkMode: window.matchMedia('(prefers-color-scheme: dark)').matches,
    };
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === null ? defaults : {...defaults, ...JSON.parse(stored)};
  }
}
