import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {Member} from '../../../../models/member.model';

export type MemberAvatarSize = 'xs' | 'sm' | 'md' | 'lg';

const NEUTRAL_COLOR = 'var(--p-text-muted-color)';

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

  private readonly existingMember = computed(() => {
    const member = this.member();
    return member?.deleted ? undefined : member;
  });

  readonly initial = computed(() => this.existingMember()?.firstName.charAt(0).toUpperCase() ?? '?');
  readonly color = computed(() => this.existingMember()?.color ?? NEUTRAL_COLOR);
}
