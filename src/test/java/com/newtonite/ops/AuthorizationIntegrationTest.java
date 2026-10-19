package com.newtonite.ops;

import com.newtonite.ops.team.Team;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthorizationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Requirement 4: User cannot read another team's work item (HTTP 403 Forbidden)")
    void testCannotReadAnotherTeamsItem() throws Exception {
        // Find an item belonging to 'Payment Services'
        Team paymentTeam = teamRepository.findByName("Payment Services").orElseThrow();
        WorkItem paymentItem = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(paymentTeam.getId()))
                .findFirst().orElseThrow();

        // Diana is only a member of 'Core Infrastructure'
        String dianaToken = getBearerTokenForUser("viewer_diana");

        mockMvc.perform(get("/api/items/" + paymentItem.getId())
                        .header("Authorization", dianaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("Requirement 5: User cannot modify another team's work item (HTTP 403 Forbidden)")
    void testCannotModifyAnotherTeamsItem() throws Exception {
        Team paymentTeam = teamRepository.findByName("Payment Services").orElseThrow();
        WorkItem paymentItem = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(paymentTeam.getId()))
                .findFirst().orElseThrow();

        // Charlie is MEMBER of 'Core Infrastructure' but has NO access to 'Payment Services'
        String charlieToken = getBearerTokenForUser("member_charlie");

        UpdateWorkItemRequest updateRequest = new UpdateWorkItemRequest(
                "Unauthorized Title Update",
                null,
                null,
                null,
                paymentItem.getVersion()
        );

        mockMvc.perform(patch("/api/items/" + paymentItem.getId())
                        .header("Authorization", charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requirement 6: Viewer cannot perform Member-only operations (HTTP 403 Forbidden)")
    void testViewerCannotPerformMemberActions() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        // Diana is VIEWER in Core Infrastructure
        String dianaToken = getBearerTokenForUser("viewer_diana");

        // 1. Cannot Create Item
        CreateWorkItemRequest createRequest = new CreateWorkItemRequest(
                "Viewer attempted create",
                "Description",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                coreInfra.getId(),
                null,
                null
        );

        mockMvc.perform(post("/api/items")
                        .header("Authorization", dianaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));

        // 2. Cannot Claim an Item
        WorkItem unassignedItem = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()) && w.getAssignee() == null && w.getStatus() != WorkItemStatus.CLOSED)
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/items/" + unassignedItem.getId() + "/claim")
                        .header("Authorization", dianaToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requirement 7: Member cannot approve a work item (HTTP 403 Forbidden)")
    void testMemberCannotApproveWorkItem() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        WorkItem itemAwaiting = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()) && w.getStatus() == WorkItemStatus.AWAITING_APPROVAL)
                .findFirst()
                .orElseGet(() -> {
                    com.newtonite.ops.user.User alice = userRepository.findByUsername("admin_alice").orElseThrow();
                    WorkItem fresh = new WorkItem(
                            UUID.randomUUID(), "Member Approval Test Target", "Desc",
                            WorkItemType.TASK, WorkItemPriority.MEDIUM, WorkItemStatus.AWAITING_APPROVAL,
                            coreInfra, null, alice, null
                    );
                    return workItemRepository.saveAndFlush(fresh);
                });

        // Charlie is MEMBER in Core Infrastructure
        String charlieToken = getBearerTokenForUser("member_charlie");

        TransitionWorkItemRequest transitionRequest = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                itemAwaiting.getVersion()
        );

        mockMvc.perform(post("/api/items/" + itemAwaiting.getId() + "/transition")
                        .header("Authorization", charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Only a team Lead or Admin can approve")));
    }

    @Test
    @DisplayName("Requirement 8: Lead can approve an eligible item in their team (HTTP 200 OK)")
    void testLeadCanApproveWorkItem() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        WorkItem itemAwaiting = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()) && w.getStatus() == WorkItemStatus.AWAITING_APPROVAL)
                .findFirst()
                .orElseGet(() -> {
                    com.newtonite.ops.user.User alice = userRepository.findByUsername("admin_alice").orElseThrow();
                    WorkItem fresh = new WorkItem(
                            UUID.randomUUID(), "Lead Approval Target", "Desc",
                            WorkItemType.TASK, WorkItemPriority.MEDIUM, WorkItemStatus.AWAITING_APPROVAL,
                            coreInfra, null, alice, null
                    );
                    return workItemRepository.saveAndFlush(fresh);
                });

        // Bob is LEAD in Core Infrastructure
        String bobToken = getBearerTokenForUser("lead_bob");

        TransitionWorkItemRequest transitionRequest = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                itemAwaiting.getVersion()
        );

        mockMvc.perform(post("/api/items/" + itemAwaiting.getId() + "/transition")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.version", is(itemAwaiting.getVersion().intValue() + 1)));
    }
}
