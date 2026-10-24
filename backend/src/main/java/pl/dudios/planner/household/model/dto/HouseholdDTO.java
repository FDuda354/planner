package pl.dudios.planner.household.model.dto;

import pl.dudios.planner.member.model.MemberDTO;

import java.util.List;

public record HouseholdDTO(
  Long id,
  String name,
  List<MemberDTO> members,
  List<MemberDTO> formerMembers,
  List<String> pendingInvitations
) {

}
