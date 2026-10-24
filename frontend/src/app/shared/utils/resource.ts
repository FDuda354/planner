import {ResourceRef, Signal} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {skip} from 'rxjs';

export function reloadWhenChanged(trigger: Signal<unknown>, resource: ResourceRef<unknown>): void {
  toObservable(trigger)
    .pipe(skip(1), takeUntilDestroyed())
    .subscribe(() => resource.reload());
}
