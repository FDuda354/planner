export interface Member {
  id: number;
  firstName: string;
  email: string;
  color: string;
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
