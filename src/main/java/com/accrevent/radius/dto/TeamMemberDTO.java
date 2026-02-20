package com.accrevent.radius.dto;

import lombok.Data;


@Data
public class TeamMemberDTO {

    private Long teamMemberId;
    private String userId;
    private Long workspaceId;

}
