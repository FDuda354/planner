import {ChangeDetectionStrategy, Component, computed, ElementRef, inject, viewChild} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {filter} from 'rxjs';
import {MenuItem} from 'primeng/api';
import {AppRoute} from '../../app-routes';
import {TransactionEditorStore} from '../../stores/transaction-editor.store';
import {monthLabelInGenitive} from '../../shared/utils/month';
import {CalendarStore} from '../../stores/calendar.store';

const TYPING_TARGETS = new Set(['INPUT', 'TEXTAREA', 'SELECT']);

@Component({
  selector: 'app-default',
  templateUrl: './default-component.html',
  styleUrl: './default-component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown)': 'onKeydown($event)',
  },
})
export class DefaultComponent {
  readonly editor = inject(TransactionEditorStore);
  private readonly calendar = inject(CalendarStore);
  private readonly router = inject(Router);
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  readonly menuItems: MenuItem[] = [
    {label: 'Budżet', icon: 'pi pi-wallet', routerLink: AppRoute.BUDGET},
    {label: 'Podsumowanie', icon: 'pi pi-chart-bar', routerLink: AppRoute.SUMMARY},
    {label: 'Cele', icon: 'pi pi-bullseye', routerLink: AppRoute.GOALS},
    {separator: true},
    {label: 'Konfiguracja', icon: 'pi pi-sliders-h', routerLink: AppRoute.SETTINGS},
  ];
  readonly navItems = this.menuItems.filter(item => !item.separator);

  readonly fabLabel = computed(() => {
    const month = this.editor.browsedMonth();
    return month !== null && month < this.calendar.currentMonth() ? `Dodaj do ${monthLabelInGenitive(month)}` : 'Dodaj';
  });

  constructor() {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.scrollContentToTop());
  }

  onKeydown(event: KeyboardEvent): void {
    if (this.isNewTransactionShortcut(event)) {
      event.preventDefault();
      this.editor.create();
    }
  }

  private scrollContentToTop(): void {
    this.content().nativeElement.scrollTo({top: 0});
  }

  private isNewTransactionShortcut(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement;
    return event.key.toLowerCase() === 'n'
      && !event.ctrlKey && !event.metaKey && !event.altKey
      && !TYPING_TARGETS.has(target.tagName)
      && !target.isContentEditable
      && !this.editor.state().visible;
  }
}
