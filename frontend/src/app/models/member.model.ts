export interface Member {
  id: number;
  firstName: string;
  email: string | null;
  color: string;
  deleted: boolean;
}

export interface Household {
  id: number;
  name: string;
  members: Member[];
  formerMembers: Member[];
  pendingInvitations: string[];
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  email: string;
  password: string;
}
