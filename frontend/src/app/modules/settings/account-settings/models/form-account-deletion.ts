import {required, schema} from '@angular/forms/signals';

export interface AccountDeletionFormModel {
  password: string;
}

export function emptyAccountDeletionModel(): AccountDeletionFormModel {
  return {password: ''};
}

export const accountDeletionSchema = schema<AccountDeletionFormModel>((path) => {
  required(path.password, {message: 'Podaj hasło, aby potwierdzić'});
});
