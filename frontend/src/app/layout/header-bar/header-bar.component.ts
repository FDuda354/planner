import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {AuthService} from '../../modules/auth/services/auth.service';
import {AppearanceService} from '../../services/appearance.service';

@Component({
  selector: 'app-header-bar',
  templateUrl: './header-bar.component.html',
  styleUrl: './header-bar.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderBarComponent {
  private readonly authService = inject(AuthService);
  readonly appearance = inject(AppearanceService);

  readonly member = this.authService.currentMember;

  logOut(): void {
    this.authService.logout();
  }
}
