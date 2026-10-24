import {ChangeDetectionStrategy, Component, inject, output, signal} from '@angular/core';
import {Router} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {apply, disabled, form} from '@angular/forms/signals';
import {finalize} from 'rxjs';
import {emptyRegisterModel, registerSchema} from './models/form-register';
import {prepareFormSubmission} from '../../../../../shared/forms/prepare-form-submission';
import {trimField} from '../../../../../shared/forms/trim-field';
import {AuthService} from '../../../services/auth.service';
import {AppRoute} from '../../../../../app-routes';
import {serverMessage} from '../../../../../utils/NotificationHelper';

@Component({
  selector: 'app-register-form',
  templateUrl: './register-form.component.html',
  styleUrl: './register-form.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterFormComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly switchToLogin = output<void>();

  private readonly registerModel = signal(emptyRegisterModel());
  readonly isWorking = signal(false);
  readonly errorMessage = signal('');
  readonly passwordMasked = signal(true);
  readonly registerForm = form(this.registerModel, (path) => {
    apply(path, registerSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  register(event: SubmitEvent): void {
    event.preventDefault();
    this.trimEmail();
    if (!prepareFormSubmission(this.registerForm, this.isWorking())) {
      return;
    }

    this.isWorking.set(true);
    this.errorMessage.set('');

    this.authService.register(this.registerModel())
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigate([AppRoute.BUDGET]);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(serverMessage(error) ?? 'Wystąpił błąd serwera'),
      });
  }

  trimEmail(): void {
    trimField(this.registerForm.email);
  }
}
