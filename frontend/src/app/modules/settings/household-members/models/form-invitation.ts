import {email, required, schema} from '@angular/forms/signals';

export interface InvitationFormModel {
  email: string;
}

export function emptyInvitationModel(): InvitationFormModel {
  return {email: ''};
}

export const invitationSchema = schema<InvitationFormModel>((path) => {
  required(path.email, {message: 'Podaj adres e-mail'});
  email(path.email, {message: 'Niepoprawny adres e-mail'});
});
