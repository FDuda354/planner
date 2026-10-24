import {computed, inject, Injectable} from '@angular/core';
import {rxResource} from '@angular/core/rxjs-interop';
import {HouseholdService} from '../services/household.service';
import {AuthService} from '../modules/auth/services/auth.service';
import {Household, Member} from '../models/Member';

const EMPTY_HOUSEHOLD: Household = {id: 0, name: '', members: [], formerMembers: [], pendingInvitations: []};

@Injectable({providedIn: 'root'})
export class HouseholdStore {
  private readonly householdService = inject(HouseholdService);
  private readonly authService = inject(AuthService);

  readonly household = rxResource<Household, number | undefined>({
    params: () => this.authService.currentMember()?.id,
    stream: () => this.householdService.getHousehold(),
    defaultValue: EMPTY_HOUSEHOLD,
  });

  readonly members = computed(() => this.household.value().members);
  readonly everyone = computed(() => [...this.members(), ...this.household.value().formerMembers]);

  memberOf(memberId: number): Member | undefined {
    return this.everyone().find(member => member.id === memberId);
  }

  isCurrentMember(memberId: number): boolean {
    return this.members().some(member => member.id === memberId);
  }

  reload(): void {
    this.household.reload();
  }
}
