import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {AppearanceService} from '../../../../services/appearance.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  readonly appearance = inject(AppearanceService);
  readonly mode = signal<'login' | 'register'>('login');
}
