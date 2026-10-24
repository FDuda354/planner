import {ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal} from '@angular/core';
import {Router} from '@angular/router';
import {form} from '@angular/forms/signals';
import {BudgetConfigStore} from '../../stores/budget-config.store';
import {AppearanceService} from '../../services/appearance.service';
import {TransactionType} from '../../models/Transaction';
import {BudgetItemEditorStore} from './stories/budget-item-editor-store';

export enum SettingsTab {
  CATEGORIES = 'categories',
  INCOMES = 'incomes',
  GOALS = 'goals',
  HOUSEHOLD = 'household',
  APPEARANCE = 'appearance',
}

interface SettingsTabOption {
  tab: SettingsTab;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BudgetItemEditorStore],
})
export class SettingsComponent {
  private readonly router = inject(Router);
  readonly configStore = inject(BudgetConfigStore);
  readonly appearance = inject(AppearanceService);

  readonly tab = input<string>();
  readonly darkModeInputId = 'settings-dark-mode';

  readonly tabs: SettingsTabOption[] = [
    {tab: SettingsTab.CATEGORIES, label: 'Kategorie wydatków', icon: 'pi pi-shopping-cart'},
    {tab: SettingsTab.INCOMES, label: 'Źródła dochodu', icon: 'pi pi-briefcase'},
    {tab: SettingsTab.GOALS, label: 'Cele', icon: 'pi pi-bullseye'},
    {tab: SettingsTab.HOUSEHOLD, label: 'Domownicy', icon: 'pi pi-users'},
    {tab: SettingsTab.APPEARANCE, label: 'Wygląd', icon: 'pi pi-palette'},
  ];

  readonly activeTab = computed(() => this.tabs.find(option => option.tab === this.tab())?.tab ?? SettingsTab.CATEGORIES);
  readonly activeLabel = computed(() => this.tabs.find(option => option.tab === this.activeTab())!.label);
  readonly themeForm = form(linkedSignal(() => ({darkMode: this.appearance.darkMode()})));

  selectTab(tab: string | number | undefined): void {
    void this.router.navigate([], {queryParams: {tab}, replaceUrl: true});
  }

  protected readonly SettingsTab = SettingsTab;
  protected readonly TransactionType = TransactionType;
}
