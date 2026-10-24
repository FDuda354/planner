import {email, required, schema} from '@angular/forms/signals';

export interface LoginFormModel {
  email: string;
  password: string;
}

export function emptyLoginModel(): LoginFormModel {
  return {email: '', password: ''};
}

export const loginSchema = schema<LoginFormModel>((path) => {
  required(path.email, {message: 'E-mail jest wymagany'});
  email(path.email, {message: 'Niepoprawny adres e-mail'});
  required(path.password, {message: 'Hasło jest wymagane'});
});
