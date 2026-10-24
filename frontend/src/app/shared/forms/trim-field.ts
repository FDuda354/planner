import {FieldTree} from '@angular/forms/signals';

export function trimField(field: FieldTree<string>): void {
  field().value.update(value => value.trim());
}
