import {NgModule} from '@angular/core';
import {SharedModule} from '../shared/shared.module';
import {DefaultComponent} from './default/default-component';
import {SideBarComponent} from './side-bar/side-bar.component';
import {HeaderBarComponent} from './header-bar/header-bar.component';

@NgModule({
  declarations: [
    DefaultComponent,
    SideBarComponent,
    HeaderBarComponent,
  ],
  imports: [
    SharedModule,
  ],
  exports: [
    DefaultComponent,
  ]
})
export class LayoutModule {
}
