import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {MonthStore} from '../../stores/month.store';
import {TransactionType} from '../../../../models/transaction.model';

type SplitKind = TransactionType.EXPENSE | TransactionType.INCOME;

interface SplitCopy {
  label: string;
  icon: string;
  title: string;
  subtitle: string;
  shareLabel: string;
  sharedName: string;
  sharedHint: string;
}

const SPLIT_COPY: Record<SplitKind, SplitCopy> = {
  [TransactionType.EXPENSE]: {
    label: 'Wydatki',
    icon: 'pi pi-arrow-up-right',
    title: 'Kto ile wydał na siebie',
    subtitle: 'Bez wydatków na dobro wspólne',
    shareLabel: 'wydatków osobistych',
    sharedName: 'Dobro wspólne',
    sharedHint: 'rachunki, czynsz, wspólne zakupy',
  },
  [TransactionType.INCOME]: {
    label: 'Wpływy',
    icon: 'pi pi-arrow-down-left',
    title: 'Kto ile wpłacił do budżetu',
    subtitle: 'Bez wspólnych wpływów',
    shareLabel: 'wpływów osobistych',
    sharedName: 'Otrzymane wspólnie',
    sharedHint: 'prezenty, zwroty — bez przypisanej osoby',
  },
};

@Component({
  selector: 'app-member-split',
  templateUrl: './member-split.component.html',
  styleUrl: './member-split.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberSplitComponent {
  private readonly store = inject(MonthStore);

  readonly kinds: SplitKind[] = [TransactionType.EXPENSE, TransactionType.INCOME];
  readonly kind = signal<SplitKind>(TransactionType.EXPENSE);
  readonly copy = computed(() => SPLIT_COPY[this.kind()]);
  readonly shares = computed(() => this.kind() === TransactionType.EXPENSE ? this.store.memberShares() : this.store.memberIncomeShares());
  readonly sharedAmount = computed(() => this.kind() === TransactionType.EXPENSE ? this.store.sharedExpenses() : this.store.sharedIncome());

  labelOf(kind: SplitKind): string {
    return SPLIT_COPY[kind].label;
  }

  iconOf(kind: SplitKind): string {
    return SPLIT_COPY[kind].icon;
  }
}
