import {inject, Injectable} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {BudgetConfigService} from '../../../services/budget-config.service';
import {TransactionService} from '../../../services/transaction.service';
import {BudgetConfigStore} from '../../../stores/budget-config.store';
import {BudgetItem, ItemUsage} from '../../../models/budget-item.model';
import {TransactionType} from '../../../models/transaction.model';
import {formatMoney} from '../../../shared/utils/money';
import {pluralPl} from '../../../shared/utils/plural';
import {showServerError, showSuccess} from '../../../shared/utils/notifications';

const DELETE_HEADERS: Record<TransactionType, string> = {
  [TransactionType.EXPENSE]: 'Usunąć kategorię?',
  [TransactionType.INCOME]: 'Usunąć źródło dochodu?',
  [TransactionType.SAVING]: 'Usunąć cel?',
};

@Injectable({providedIn: 'root'})
export class BudgetItemRemoverService {
  private readonly configService = inject(BudgetConfigService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly transactionService = inject(TransactionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  confirmDeletion(kind: TransactionType, item: BudgetItem, onDeleted?: () => void): void {
    this.configService.getItemUsage(kind, item.id).subscribe({
      next: (usage) => this.confirmationService.confirm({
        key: 'deleteConfirmDialog',
        header: DELETE_HEADERS[kind],
        message: deletionMessage(item, usage),
        icon: 'pi pi-trash',
        acceptLabel: 'Usuń',
        rejectLabel: 'Anuluj',
        acceptButtonStyleClass: 'p-button-danger',
        rejectButtonStyleClass: 'p-button-text',
        accept: () => this.delete(kind, item, onDeleted),
      }),
      error: (error) => showServerError('Nie udało się sprawdzić, co zostanie usunięte', error, this.messageService),
    });
  }

  private delete(kind: TransactionType, item: BudgetItem, onDeleted?: () => void): void {
    this.configService.deleteItem(kind, item.id).subscribe({
      next: () => {
        showSuccess('Usunięto', item.name, this.messageService);
        this.configStore.reload();
        this.transactionService.markChanged();
        onDeleted?.();
      },
      error: (error) => showServerError('Nie udało się usunąć', error, this.messageService),
    });
  }
}

function deletionMessage(item: BudgetItem, usage: ItemUsage): string {
  if (usage.transactionCount === 0) {
    return `„${item.name}” zniknie z konfiguracji. Tego nie da się cofnąć.`;
  }
  const transactions = `${usage.transactionCount} ${pluralPl(usage.transactionCount, 'transakcję', 'transakcje', 'transakcji')}`;
  return `Razem z „${item.name}” usuniesz ${transactions} na łączną kwotę ${formatMoney(usage.totalAmount)}. `
    + 'Znikną też z podsumowań poprzednich miesięcy i tego nie da się cofnąć.';
}
