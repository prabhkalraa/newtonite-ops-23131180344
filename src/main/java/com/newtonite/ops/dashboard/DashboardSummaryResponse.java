package com.newtonite.ops.dashboard;

import com.newtonite.ops.workitem.WorkItemResponse;
import java.util.List;

public class DashboardSummaryResponse {
    private long totalOpenItems;
    private long assignedToMe;
    private long unassignedUrgent;
    private long awaitingApproval;
    private List<WorkItemResponse> recentItems;

    public DashboardSummaryResponse() {}

    public DashboardSummaryResponse(long totalOpenItems, long assignedToMe,
                                    long unassignedUrgent, long awaitingApproval,
                                    List<WorkItemResponse> recentItems) {
        this.totalOpenItems = totalOpenItems;
        this.assignedToMe = assignedToMe;
        this.unassignedUrgent = unassignedUrgent;
        this.awaitingApproval = awaitingApproval;
        this.recentItems = recentItems;
    }

    public long getTotalOpenItems() {
        return totalOpenItems;
    }

    public void setTotalOpenItems(long totalOpenItems) {
        this.totalOpenItems = totalOpenItems;
    }

    public long getAssignedToMe() {
        return assignedToMe;
    }

    public void setAssignedToMe(long assignedToMe) {
        this.assignedToMe = assignedToMe;
    }

    public long getUnassignedUrgent() {
        return unassignedUrgent;
    }

    public void setUnassignedUrgent(long unassignedUrgent) {
        this.unassignedUrgent = unassignedUrgent;
    }

    public long getAwaitingApproval() {
        return awaitingApproval;
    }

    public void setAwaitingApproval(long awaitingApproval) {
        this.awaitingApproval = awaitingApproval;
    }

    public List<WorkItemResponse> getRecentItems() {
        return recentItems;
    }

    public void setRecentItems(List<WorkItemResponse> recentItems) {
        this.recentItems = recentItems;
    }
}
