import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {FormField, FormRoot} from '@angular/forms/signals';
import {ButtonDirective} from 'primeng/button';
import {Ripple} from 'primeng/ripple';
import {Toast} from 'primeng/toast';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ConfirmPopup} from 'primeng/confirmpopup';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {InputPassword} from 'primeng/inputpassword';
import {Textarea} from 'primeng/textarea';
import {DatePicker} from 'primeng/datepicker';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {AutoFocus} from 'primeng/autofocus';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {SelectButton} from 'primeng/selectbutton';
import {Tab, TabList, Tabs} from 'primeng/tabs';
import {Tooltip} from 'primeng/tooltip';
import {Skeleton} from 'primeng/skeleton';
import {Menu} from 'primeng/menu';
import {Tag} from 'primeng/tag';
import {Divider} from 'primeng/divider';
import {Message} from 'primeng/message';
import {Checkbox} from 'primeng/checkbox';
import {MoneyPipe} from './pipes/money.pipe';
import {ChartDirective} from './components/ui/chart/chart.directive';
import {ItemIconComponent} from './components/ui/item-icon/item-icon.component';
import {MemberAvatarComponent} from './components/ui/member-avatar/member-avatar.component';
import {ProgressRingComponent} from './components/ui/progress-ring/progress-ring.component';
import {EmptyStateComponent} from './components/ui/empty-state/empty-state.component';
import {TransactionDialogComponent} from './components/transaction-dialog/transaction-dialog.component';

const PRIME_COMPONENTS = [
  ButtonDirective, Ripple, Toast, ConfirmDialog, ConfirmPopup, Dialog, InputText, InputNumber, InputPassword, Textarea,
  DatePicker, IconField, InputIcon, AutoFocus, ToggleSwitch, SelectButton, Tabs, TabList, Tab, Tooltip, Skeleton, Menu,
  Tag, Divider, Message, Checkbox,
];

const SHARED_DECLARATIONS = [
  MoneyPipe, ChartDirective, ItemIconComponent, MemberAvatarComponent, ProgressRingComponent, EmptyStateComponent,
  TransactionDialogComponent,
];

@NgModule({
  declarations: [...SHARED_DECLARATIONS],
  imports: [
    CommonModule,
    RouterModule,
    FormField,
    FormRoot,
    ...PRIME_COMPONENTS,
  ],
  exports: [
    CommonModule,
    RouterModule,
    FormField,
    FormRoot,
    ...PRIME_COMPONENTS,
    ...SHARED_DECLARATIONS,
  ]
})
export class SharedModule {
}
