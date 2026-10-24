import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {Member} from '../../../../models/Member';

export type MemberAvatarSize = 'xs' | 'sm' | 'md' | 'lg';

@Component({
  selector: 'ui-member-avatar',
  templateUrl: './member-avatar.component.html',
  styleUrl: './member-avatar.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberAvatarComponent {
  readonly member = input.required<Member | undefined>();
  readonly size = input<MemberAvatarSize>('md');

  readonly initial = computed(() => this.member()?.firstName.charAt(0).toUpperCase() ?? '?');
}
