package com.accrevent.radius.controller;


import com.accrevent.radius.dto.TeamMemberDTO;

import com.accrevent.radius.service.TeamMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/team")

public class TeamMemberController {
    private static final Logger logger = LoggerFactory.getLogger(TeamMemberController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
   
    private final TeamMemberService teamMemberService;

    public TeamMemberController(TeamMemberService teamMemberService)
    {
        this.teamMemberService = teamMemberService;
    }

    @PutMapping("/addMember")
    public ResponseEntity<Map<String,Object>> addTeamMember(@RequestBody TeamMemberDTO teamMemberDTO)
    {
        try
        {
            TeamMemberDTO createdTeamMember = teamMemberService.addTeamMember(teamMemberDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("AddedMember", createdTeamMember);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Team Member Added successfully.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteTeam(@RequestParam Long teamId){

        try {
            System.out.println("teamid:____"+teamId);
            boolean isDeleted = teamMemberService.deleteTeamMember(teamId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, teamId + " Team member successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete team member with ID: {}", teamId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Team member.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Team member ID: {} not found", teamId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Team member not found."); // 404 Not Found with response body

        }
    }

    @PostMapping("/getTeamMemberByWorkspaceId")
    public ResponseEntity<Map<String,Object>> getTeamByWorkspaceId(@RequestParam Long workspaceId){
        try{
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("TeamMembers", teamMemberService.getTeamByWorkspaceId(workspaceId));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get Team Member successfully.", responseBody);
        }catch(Exception e)
        {
            logger.warn("getTeamByWorkspaceId purpose given Workspace ID: {} not exist", workspaceId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage()); // 404 Not Found with response body

        }
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
        return buildResponse(status, responseStatus, message, new HashMap<>());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", responseStatus);
        response.put("message", message);
        response.putAll(additionalData);
        return ResponseEntity.status(status).body(response);
    }

}
