import {FieldTree} from '@angular/forms/signals';

export function prepareFormSubmission<T>(form: FieldTree<T>, submitting: boolean): boolean {
  if (submitting || form().disabled()) {
    return false;
  }

  form().markAsTouched();
  if (!form().valid()) {
    form().errorSummary()[0]?.fieldTree().focusBoundControl();
    return false;
  }

  return true;
}
