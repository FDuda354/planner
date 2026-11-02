import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {Member} from '../../../../models/member.model';

@Component({
  selector: 'ui-member-picker',
  templateUrl: './member-picker.component.html',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberPickerComponent {
  readonly value = input.required<number | null>();
  readonly members = input.required<Member[]>();
  readonly name = input.required<string>();
  readonly label = input.required<string>();
  readonly noneLabel = input<string | null>(null);
  readonly noneIcon = input<string | null>(null);
  readonly changed = output<number | null>();
}
