import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {apply, disabled, form} from '@angular/forms/signals';
import {ConfirmationService, MessageService} from 'primeng/api';
import {finalize} from 'rxjs';
import {HouseholdStore} from '../../../stores/household.store';
import {HouseholdService} from '../../../services/household.service';
import {AuthService} from '../../auth/services/auth.service';
import {Member} from '../../../models/Member';
import {prepareFormSubmission} from '../../../shared/forms/prepare-form-submission';
import {trimField} from '../../../shared/forms/trim-field';
import {showServerError, showSuccess} from '../../../utils/NotificationHelper';
import {emptyInvitationModel, invitationSchema} from './models/form-invitation';

@Component({
  selector: 'app-household-members',
  templateUrl: './household-members.component.html',
  styleUrl: './household-members.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HouseholdMembersComponent {
  readonly householdStore = inject(HouseholdStore);
  private readonly householdService = inject(HouseholdService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  readonly currentMember = inject(AuthService).currentMember;

  readonly isWorking = signal(false);
  private readonly invitationModel = signal(emptyInvitationModel());
  readonly invitationForm = form(this.invitationModel, (path) => {
    apply(path, invitationSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  invite(event: SubmitEvent): void {
    event.preventDefault();
    this.trimEmail();
    if (!prepareFormSubmission(this.invitationForm, this.isWorking())) {
      return;
    }
    const email = this.invitationModel().email;
    this.isWorking.set(true);
    this.householdService.inviteMember(email)
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          showSuccess('Dodano zaproszenie', email, this.messageService);
          this.invitationForm().reset(emptyInvitationModel());
          this.householdStore.reload();
        },
        error: (error) => showServerError('Nie udało się zaprosić', error, this.messageService),
      });
  }

  trimEmail(): void {
    trimField(this.invitationForm.email);
  }

  removeMember(member: Member): void {
    this.confirmationService.confirm({
      key: 'deleteConfirmDialog',
      header: 'Usunąć domownika?',
      message: `${member.firstName} straci dostęp do wspólnego budżetu. Transakcje dodane z tego konta zostaną w historii, a dostęp możesz później przywrócić.`,
      icon: 'pi pi-user-minus',
      acceptLabel: 'Usuń z budżetu',
      rejectLabel: 'Anuluj',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.householdService.removeMember(member.id).subscribe({
          next: () => {
            showSuccess('Usunięto z budżetu', member.firstName, this.messageService);
            this.householdStore.reload();
          },
          error: (error) => showServerError('Nie udało się usunąć domownika', error, this.messageService),
        });
      },
    });
  }

  restoreMember(member: Member): void {
    this.householdService.restoreMember(member.id).subscribe({
      next: () => {
        showSuccess('Przywrócono dostęp', member.firstName, this.messageService);
        this.householdStore.reload();
      },
      error: (error) => showServerError('Nie udało się przywrócić dostępu', error, this.messageService),
    });
  }

  cancelInvitation(email: string): void {
    this.householdService.cancelInvitation(email).subscribe({
      next: () => {
        showSuccess('Anulowano zaproszenie', email, this.messageService);
        this.householdStore.reload();
      },
      error: (error) => showServerError('Nie udało się anulować zaproszenia', error, this.messageService),
    });
  }
}
