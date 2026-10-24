import {NgModule} from '@angular/core';
import {SharedModule} from '../../shared/shared.module';
import {LoginComponent} from './views/login/login.component';
import {LoginFormComponent} from './views/login/login-form/login-form.component';
import {RegisterFormComponent} from './views/login/register-form/register-form.component';

@NgModule({
  declarations: [LoginComponent, LoginFormComponent, RegisterFormComponent],
  imports: [
    SharedModule,
  ]
})
export class AuthModule {
}
