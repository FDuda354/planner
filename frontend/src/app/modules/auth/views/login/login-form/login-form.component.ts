import {ChangeDetectionStrategy, Component, inject, output, signal} from '@angular/core';
import {Router} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {apply, disabled, form} from '@angular/forms/signals';
import {finalize} from 'rxjs';
import {emptyLoginModel, loginSchema} from './models/form-login';
import {prepareFormSubmission} from '../../../../../shared/forms/prepare-form-submission';
import {trimField} from '../../../../../shared/forms/trim-field';
import {AuthService} from '../../../services/auth.service';
import {AppRoute} from '../../../../../app-routes';
import {serverMessage} from '../../../../../utils/NotificationHelper';

@Component({
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrl: './login-form.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly switchToRegister = output<void>();

  private readonly loginModel = signal(emptyLoginModel());
  readonly isWorking = signal(false);
  readonly errorMessage = signal('');
  readonly passwordMasked = signal(true);
  readonly loginForm = form(this.loginModel, (path) => {
    apply(path, loginSchema);
    disabled(path, {when: () => this.isWorking()});
  });

  login(event: SubmitEvent): void {
    event.preventDefault();
    this.trimEmail();
    if (!prepareFormSubmission(this.loginForm, this.isWorking())) {
      return;
    }

    this.isWorking.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginModel())
      .pipe(finalize(() => this.isWorking.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigate([AppRoute.BUDGET]);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(serverMessage(error) ?? 'Wystąpił błąd serwera'),
      });
  }

  trimEmail(): void {
    trimField(this.loginForm.email);
  }
}
