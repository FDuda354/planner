import {email, maxLength, minLength, required, schema} from '@angular/forms/signals';

export interface RegisterFormModel {
  firstName: string;
  email: string;
  password: string;
}

export function emptyRegisterModel(): RegisterFormModel {
  return {firstName: '', email: '', password: ''};
}

export const registerSchema = schema<RegisterFormModel>((path) => {
  required(path.firstName, {message: 'Podaj imię'});
  maxLength(path.firstName, 60, {message: 'Imię może mieć maksymalnie 60 znaków'});
  required(path.email, {message: 'E-mail jest wymagany'});
  email(path.email, {message: 'Niepoprawny adres e-mail'});
  required(path.password, {message: 'Hasło jest wymagane'});
  minLength(path.password, 8, {message: 'Hasło musi mieć co najmniej 8 znaków'});
});
