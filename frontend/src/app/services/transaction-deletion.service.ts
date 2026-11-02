import {inject, Injectable} from '@angular/core';
import {ConfirmationService, MessageService} from 'primeng/api';
import {TransactionService} from './transaction.service';
import {BudgetConfigStore} from '../stores/budget-config.store';
import {Transaction} from '../models/Transaction';
import {transactionLabel} from '../shared/utils/transaction-label';
import {showServerError, showSuccess} from '../utils/NotificationHelper';

@Injectable({providedIn: 'root'})
export class TransactionDeletionService {
  private readonly transactionService = inject(TransactionService);
  private readonly configStore = inject(BudgetConfigStore);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  confirmDeletion(transaction: Transaction, target: EventTarget | null, onDeleted?: () => void): void {
    this.confirmationService.confirm({
      target: target ?? undefined,
      message: 'Na pewno usunąć tę transakcję?',
      icon: 'pi pi-trash',
      acceptLabel: 'Usuń',
      rejectLabel: 'Anuluj',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-text p-button-sm',
      accept: () => this.delete(transaction, onDeleted),
    });
  }

  private delete(transaction: Transaction, onDeleted?: () => void): void {
    this.transactionService.deleteTransaction(transaction.id).subscribe({
      next: () => {
        showSuccess('Usunięto transakcję', this.describe(transaction), this.messageService);
        onDeleted?.();
      },
      error: (error) => showServerError('Nie udało się usunąć transakcji', error, this.messageService),
    });
  }

  private describe(transaction: Transaction): string {
    return transactionLabel(this.configStore.itemOf(transaction.type, transaction.itemId)?.name, transaction.amount);
  }
}
