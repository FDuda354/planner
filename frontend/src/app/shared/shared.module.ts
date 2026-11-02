import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {FormField, FormRoot} from '@angular/forms/signals';
import {ButtonDirective} from 'primeng/button';
import {Toast} from 'primeng/toast';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ConfirmPopup} from 'primeng/confirmpopup';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {InputNumber} from 'primeng/inputnumber';
import {InputPassword} from 'primeng/inputpassword';
import {DatePicker} from 'primeng/datepicker';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {AutoFocus} from 'primeng/autofocus';
import {ToggleSwitch} from 'primeng/toggleswitch';
import {Select} from 'primeng/select';
import {Tab, TabList, Tabs} from 'primeng/tabs';
import {Tooltip} from 'primeng/tooltip';
import {Skeleton} from 'primeng/skeleton';
import {Tag} from 'primeng/tag';
import {Divider} from 'primeng/divider';
import {Message} from 'primeng/message';
import {Checkbox} from 'primeng/checkbox';
import {MoneyPipe} from './pipes/money.pipe';
import {ChartDirective} from './components/ui/chart/chart.directive';
import {ItemIconComponent} from './components/ui/item-icon/item-icon.component';
import {MemberAvatarComponent} from './components/ui/member-avatar/member-avatar.component';
import {TypeFilterComponent} from './components/filters/type-filter/type-filter.component';
import {MemberPickerComponent} from './components/ui/member-picker/member-picker.component';
import {SharedFilterComponent} from './components/filters/shared-filter/shared-filter.component';
import {ProgressRingComponent} from './components/ui/progress-ring/progress-ring.component';
import {EmptyStateComponent} from './components/ui/empty-state/empty-state.component';
import {FieldErrorComponent} from './components/ui/field-error/field-error.component';
import {TransactionDialogComponent} from './components/transaction-dialog/transaction-dialog.component';
import {TypeSwitchComponent} from './components/transaction-form/type-switch/type-switch.component';
import {ItemPickerComponent} from './components/transaction-form/item-picker/item-picker.component';
import {SharedToggleComponent} from './components/transaction-form/shared-toggle/shared-toggle.component';

const PRIME_COMPONENTS = [
  ButtonDirective, Toast, ConfirmDialog, ConfirmPopup, Dialog, InputText, InputNumber, InputPassword, DatePicker, IconField,
  InputIcon, AutoFocus, ToggleSwitch, Select, Tabs, TabList, Tab, Tooltip, Skeleton, Tag, Divider, Message, Checkbox,
];

const SHARED_DECLARATIONS = [
  MoneyPipe, ChartDirective, ItemIconComponent, MemberAvatarComponent, ProgressRingComponent, EmptyStateComponent, FieldErrorComponent,
  TransactionDialogComponent, TypeFilterComponent, MemberPickerComponent, SharedFilterComponent,
  TypeSwitchComponent, ItemPickerComponent, SharedToggleComponent,
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
