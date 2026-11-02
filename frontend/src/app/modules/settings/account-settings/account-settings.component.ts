import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {Router} from '@angular/router';
import {apply, disabled, form} from '@angular/forms/signals';
import {MessageService} from 'primeng/api';
import {finalize, Observable} from 'rxjs';
import {AccountService} from '../../../services/account.service';
import {AuthService} from '../../auth/services/auth.service';
import {HouseholdStore} from '../../../stores/household.store';
import {prepareFormSubmission} from '../../../shared/forms/prepare-form-submission';
import {serverMessage, showSuccess} from '../../../shared/utils/notifications';
import {AppRoute} from '../../../app-routes';
import {accountDeletionSchema, emptyAccountDeletionModel} from './models/form-account-deletion';

export enum AccountDeletionScope {
  ACCOUNT = 'ACCOUNT',
  ACCOUNT_AND_HOUSEHOLD = 'ACCOUNT_AND_HOUSEHOLD',
}

interface DeletionCopy {
  header: string;
  explanation: string;
  confirmLabel: string;
  successTitle: string;
}

const DELETION_COPY: Record<AccountDeletionScope, DeletionCopy> = {
  [AccountDeletionScope.ACCOUNT]: {
    header: 'Usunąć Twoje konto?',
    explanation: 'Twoje imię, e-mail i hasło zostaną usunięte, a Ty stracisz dostęp do budżetu. Transakcje dodane z Twojego konta zostaną w nim jako „Usunięte konto”. Tego nie da się cofnąć.',
    confirmLabel: 'Usuń konto',
    successTitle: 'Usunięto konto',
  },
  [AccountDeletionScope.ACCOUNT_AND_HOUSEHOLD]: {
    header: 'Usunąć konto i budżet?',
    explanation: 'Usuniemy Twoje konto i cały budżet: wszystkie transakcje, kategorie, źródła dochodu, cele i zaproszenia. Tego nie da się cofnąć.',
    confirmLabel: 'Usuń konto i budżet',
    successTitle: 'Usunięto konto i budżet',
  },
};

@Component({
  selector: 'app-account-settings',
  templateUrl: './account-settings.component.html',
  styleUrl: './account-settings.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountSettingsComponent {
  private readonly accountService = inject(AccountService);
  private readonly authService = inject(AuthService);
  private readonly householdStore = inject(HouseholdStore);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  readonly currentMember = this.authService.currentMember;
  readonly passwordInputId = 'account-deletion-password';
  readonly otherMembers = computed(() => this.householdStore.members().filter(member => member.id !== this.currentMember()?.id));
  readonly isOnlyMember = computed(() => this.otherMembers().length === 0);
  readonly otherMemberNames = computed(() => this.otherMembers().map(member => member.firstName).join(', '));

  readonly scope = signal<AccountDeletionScope | null>(null);
  readonly copy = computed(() => {
    const scope = this.scope();
    return scope === null ? null : DELETION_COPY[scope];
  });
  readonly isWorking = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly passwordMasked = signal(true);

  private readonly deletionModel = signal(emptyAccountDeletionModel());
  readonly deletionForm = form(this.deletionModel, (path) => {
    apply(path, accountDeletionSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  open(scope: AccountDeletionScope): void {
    this.errorMessage.set(null);
    this.passwordMasked.set(true);
    this.deletionForm().reset(emptyAccountDeletionModel());
    this.scope.set(scope);
  }

  onVisibleChange(visible: boolean): void {
    if (!visible) {
      this.scope.set(null);
    }
  }

  confirm(event: SubmitEvent): void {
    event.preventDefault();
    const scope = this.scope();
    if (scope === null || !prepareFormSubmission(this.deletionForm, this.isWorking())) {
      return;
    }
    this.isWorking.set(true);
    this.errorMessage.set(null);
    this.deletionRequest(scope, this.deletionModel().password)
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => this.leaveDeletedAccount(DELETION_COPY[scope].successTitle),
        error: (error) => this.errorMessage.set(serverMessage(error) ?? 'Nie udało się usunąć konta — spróbuj ponownie za chwilę'),
      });
  }

  private deletionRequest(scope: AccountDeletionScope, password: string): Observable<void> {
    return scope === AccountDeletionScope.ACCOUNT
      ? this.accountService.deleteAccount(password)
      : this.accountService.deleteAccountWithHousehold(password);
  }

  private leaveDeletedAccount(successTitle: string): void {
    this.scope.set(null);
    this.authService.clear();
    showSuccess(successTitle, 'Do zobaczenia!', this.messageService);
    void this.router.navigate([AppRoute.LOGIN]);
  }

  protected readonly AccountDeletionScope = AccountDeletionScope;
}
