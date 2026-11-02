import {inject, LOCALE_ID, NgModule, provideAppInitializer, provideZonelessChangeDetection} from '@angular/core';
import {registerLocaleData} from '@angular/common';
import localePl from '@angular/common/locales/pl';
import {BrowserModule} from '@angular/platform-browser';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {provideSignalFormsConfig} from '@angular/forms/signals';
import {ConfirmationService, MessageService} from 'primeng/api';
import {providePrimeNG} from 'primeng/config';
import {AppComponent} from './app.component';
import {BUDGET_PRESET, POLISH_TRANSLATION} from './config/primeng.config';
import {AppRoutingModule} from './app-routing.module';
import {SharedModule} from './shared/shared.module';
import {LayoutModule} from './layout/layout.module';
import {AuthModule} from './modules/auth/auth.module';
import {AppearanceService} from './services/appearance.service';
import {httpInterceptorFn} from './modules/auth/services/http-interceptor.service';

declare const PRIMEUI_LICENSE: string | undefined;

registerLocaleData(localePl);

@NgModule({
  declarations: [
    AppComponent,
  ],
  bootstrap: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    SharedModule,
    LayoutModule,
    AuthModule,
  ],
  providers: [
    provideZonelessChangeDetection(),
    provideAppInitializer(() => {
      inject(AppearanceService);
    }),
    provideSignalFormsConfig({
      classes: {
        'ng-touched': ({state}) => state().touched(),
        'ng-untouched': ({state}) => !state().touched(),
      },
    }),
    {provide: LOCALE_ID, useValue: 'pl'},
    ConfirmationService,
    MessageService,
    provideHttpClient(withInterceptors([httpInterceptorFn])),
    providePrimeNG({
      license: PRIMEUI_LICENSE ?? '',
      ripple: true,
      theme: {
        preset: BUDGET_PRESET,
        options: {
          darkModeSelector: '.my-app-dark',
        }
      },
      translation: POLISH_TRANSLATION
    })
  ]
})
export class AppModule {
}
