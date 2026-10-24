import {inject, LOCALE_ID, NgModule, provideAppInitializer, provideZonelessChangeDetection} from '@angular/core';
import {registerLocaleData} from '@angular/common';
import localePl from '@angular/common/locales/pl';
import {BrowserModule} from '@angular/platform-browser';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {provideSignalFormsConfig} from '@angular/forms/signals';
import {ConfirmationService, MessageService} from 'primeng/api';
import {providePrimeNG} from 'primeng/config';
import {definePreset} from '@primeuix/themes';
import Aura from '@primeuix/themes/aura-compat';
import {AppComponent} from './app.component';
import {AppRoutingModule} from './app-routing.module';
import {SharedModule} from './shared/shared.module';
import {LayoutModule} from './layout/layout.module';
import {AuthModule} from './modules/auth/auth.module';
import {AppearanceService} from './services/appearance.service';
import {httpInterceptorFn} from './modules/auth/services/http-interceptor.service';

declare const PRIMEUI_LICENSE: string | undefined;

registerLocaleData(localePl);

const BudgetPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#effaf6',
      100: '#d7f3e8',
      200: '#b0e6d3',
      300: '#7dd3b8',
      400: '#47b998',
      500: '#239f7e',
      600: '#158066',
      700: '#136754',
      800: '#135244',
      900: '#11443a',
      950: '#072721'
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f6f8f7',
          100: '#eef2f0',
          200: '#e3e8e6',
          300: '#cfd7d4',
          400: '#9aa8a3',
          500: '#6b7a75',
          600: '#4f5d58',
          700: '#3a4642',
          800: '#25302d',
          900: '#18211f',
          950: '#0e1513'
        },
        primary: {
          color: '{primary.600}',
          contrastColor: '{surface.0}',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}'
        },
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}'
        },
        content: {
          background: '{surface.0}',
          hoverBackground: '{surface.100}',
          borderColor: '{surface.200}',
          color: '{surface.800}',
          hoverColor: '{surface.900}'
        }
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '#eef2f0',
          100: '#d5dcd9',
          200: '#b3bfba',
          300: '#8d9b96',
          400: '#6b7a75',
          500: '#4b5955',
          600: '#34413d',
          700: '#24322f',
          800: '#182326',
          900: '#131c1f',
          950: '#0b1113'
        },
        primary: {
          color: '{primary.400}',
          contrastColor: '{surface.950}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}'
        },
        highlight: {
          background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
          color: '{primary.200}',
          focusColor: '{primary.100}'
        },
        content: {
          background: '{surface.900}',
          hoverBackground: '{surface.800}',
          borderColor: '{surface.700}',
          color: '{surface.50}',
          hoverColor: '{surface.0}'
        }
      }
    }
  }
});

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
        preset: BudgetPreset,
        options: {
          darkModeSelector: '.my-app-dark',
        }
      },
      translation: {
        accept: 'Tak',
        reject: 'Nie',
        choose: 'Wybierz',
        cancel: 'Anuluj',
        dayNames: ['Niedziela', 'Poniedziałek', 'Wtorek', 'Środa', 'Czwartek', 'Piątek', 'Sobota'],
        dayNamesShort: ['Nie', 'Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob'],
        dayNamesMin: ['Nd', 'Pn', 'Wt', 'Śr', 'Cz', 'Pt', 'Sb'],
        monthNames: ['Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec', 'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień'],
        monthNamesShort: ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz', 'Paź', 'Lis', 'Gru'],
        today: 'Dzisiaj',
        clear: 'Wyczyść',
        weekHeader: 'Tydz',
        firstDayOfWeek: 1,
        dateFormat: 'dd.mm.yy'
      }
    })
  ]
})
export class AppModule {
}
