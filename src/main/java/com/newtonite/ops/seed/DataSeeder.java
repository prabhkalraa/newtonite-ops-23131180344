package com.newtonite.ops.seed;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.activity.ActivityEventType;
import com.newtonite.ops.comment.Comment;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final WorkItemRepository workItemRepository;
    private final ActivityEventRepository activityEventRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMembershipRepository teamMembershipRepository,
            WorkItemRepository workItemRepository,
            ActivityEventRepository activityEventRepository,
            CommentRepository commentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.workItemRepository = workItemRepository;
        this.activityEventRepository = activityEventRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping DataSeeder.");
            return;
        }

        log.info("Seeding initial demo data for Newtonite Operations Under Pressure...");

        String defaultHash = passwordEncoder.encode("Password123!");

        // 1. Seed Users (10 users)
        User adminAlice = createUser("admin_alice", "Alice Admin", "alice@newtonite.internal", defaultHash);
        User leadBob = createUser("lead_bob", "Bob Lead", "bob@newtonite.internal", defaultHash);
        User memberCharlie = createUser("member_charlie", "Charlie Member", "charlie@newtonite.internal", defaultHash);
        User viewerDiana = createUser("viewer_diana", "Diana Viewer", "diana@newtonite.internal", defaultHash);

        User leadElena = createUser("lead_elena", "Elena Lead", "elena@newtonite.internal", defaultHash);
        User memberFrank = createUser("member_frank", "Frank Member", "frank@newtonite.internal", defaultHash);

        User leadGrace = createUser("lead_grace", "Grace Lead", "grace@newtonite.internal", defaultHash);
        User memberHarry = createUser("member_harry", "Harry Member", "harry@newtonite.internal", defaultHash);
        User viewerIris = createUser("viewer_iris", "Iris Viewer", "iris@newtonite.internal", defaultHash);

        User memberJack = createUser("member_jack", "Jack Support", "jack@newtonite.internal", defaultHash);

        // 2. Seed Teams (4 teams)
        Team coreInfra = createTeam("Core Infrastructure", "Platform, cloud infrastructure, observability, and networking");
        Team paymentServices = createTeam("Payment Services", "Payment gateways, ledger reconciliation, and merchant settlement");
        Team complianceRisk = createTeam("Compliance & Risk", "Regulatory compliance, SOC2/ISO audits, and financial crime risk");
        Team customerSupport = createTeam("Customer Support", "Escalations, tier-3 customer incidents, and operational triage");

        // 3. Seed Team Memberships
        createMembership(adminAlice, coreInfra, TeamRole.ADMIN);
        createMembership(adminAlice, paymentServices, TeamRole.ADMIN);
        createMembership(leadBob, coreInfra, TeamRole.LEAD);
        createMembership(memberCharlie, coreInfra, TeamRole.MEMBER);
        createMembership(viewerDiana, coreInfra, TeamRole.VIEWER);

        createMembership(leadElena, paymentServices, TeamRole.LEAD);
        createMembership(memberFrank, paymentServices, TeamRole.MEMBER);

        createMembership(leadGrace, complianceRisk, TeamRole.LEAD);
        createMembership(memberHarry, complianceRisk, TeamRole.MEMBER);
        createMembership(viewerIris, complianceRisk, TeamRole.VIEWER);

        createMembership(memberJack, customerSupport, TeamRole.MEMBER);
        createMembership(leadBob, customerSupport, TeamRole.LEAD);

        // 4. Seed Work Items (> 25 items across different teams, statuses, priorities)
        Instant now = Instant.now();

        // Core Infrastructure Items
        createItem("Kubernetes ingress controller SSL certificate expiring",
                "Wildcard certificate *.newtonite.internal expires in 48 hours. Renewal required.",
                WorkItemType.INCIDENT, WorkItemPriority.URGENT, WorkItemStatus.OPEN,
                coreInfra, null, adminAlice, now.plus(2, ChronoUnit.DAYS));

        createItem("Production Kafka cluster broker disk pressure alert",
                "Broker 3 reported 92% disk utilization due to uncompacted consumer offset topics.",
                WorkItemType.INCIDENT, WorkItemPriority.HIGH, WorkItemStatus.IN_PROGRESS,
                coreInfra, memberCharlie, leadBob, now.plus(1, ChronoUnit.DAYS));

        createItem("Upgrade PostgreSQL read-replica connection pooling",
                "Increase max client pool size from 150 to 300 to handle peak traffic.",
                WorkItemType.TASK, WorkItemPriority.MEDIUM, WorkItemStatus.AWAITING_APPROVAL,
                coreInfra, memberCharlie, memberCharlie, now.plus(5, ChronoUnit.DAYS));

        createItem("Provision isolated staging environment for Q4 release",
                "Set up Terraform state, VPC peering, and deploy base service mesh.",
                WorkItemType.TASK, WorkItemPriority.LOW, WorkItemStatus.RESOLVED,
                coreInfra, leadBob, adminAlice, now.minus(1, ChronoUnit.DAYS));

        createItem("Decommission legacy Redis 5 caching nodes",
                "Migrated all cache consumers to Redis 7 cluster. Old instances can be terminated.",
                WorkItemType.TASK, WorkItemPriority.LOW, WorkItemStatus.CLOSED,
                coreInfra, memberCharlie, leadBob, now.minus(5, ChronoUnit.DAYS));

        createItem("DNS latency spike in us-east-1 region",
                "Intermittent DNS resolution timeouts affecting internal microservice mesh.",
                WorkItemType.INCIDENT, WorkItemPriority.URGENT, WorkItemStatus.OPEN,
                coreInfra, null, leadBob, now.plus(1, ChronoUnit.DAYS));

        createItem("Automate weekly backup verification tests",
                "Implement automated script to restore PostgreSQL snapshot into scratch database weekly.",
                WorkItemType.TASK, WorkItemPriority.MEDIUM, WorkItemStatus.OPEN,
                coreInfra, null, adminAlice, now.plus(7, ChronoUnit.DAYS));

        // Payment Services Items
        createItem("Stripe webhook signature validation failures",
                "Multiple charges failed settlement due to clock skew during signature check.",
                WorkItemType.PAYMENT, WorkItemPriority.URGENT, WorkItemStatus.AWAITING_APPROVAL,
                paymentServices, memberFrank, memberFrank, now.plus(1, ChronoUnit.DAYS));

        createItem("Settlement discrepancy on EUR currency ledger",
                "End-of-day batch reconciliation detected EUR 420.50 variance on escrow ledger.",
                WorkItemType.PAYMENT, WorkItemPriority.HIGH, WorkItemStatus.IN_PROGRESS,
                paymentServices, memberFrank, leadElena, now.plus(3, ChronoUnit.DAYS));

        createItem("Merchant payout batch retry mechanism",
                "Implement idempotent retry logic when acquiring bank gateway returns 504 timeout.",
                WorkItemType.PAYMENT, WorkItemPriority.HIGH, WorkItemStatus.OPEN,
                paymentServices, null, leadElena, now.plus(4, ChronoUnit.DAYS));

        createItem("Add support for UPI QR payments in Southeast Asia expansion",
                "Integrate partner API specification and build webhook handler.",
                WorkItemType.PAYMENT, WorkItemPriority.MEDIUM, WorkItemStatus.OPEN,
                paymentServices, null, adminAlice, now.plus(14, ChronoUnit.DAYS));

        createItem("Fix rounding precision error on multi-item tax calculation",
                "Rounding after summing versus before summing causes 1 cent discrepancies.",
                WorkItemType.PAYMENT, WorkItemPriority.MEDIUM, WorkItemStatus.RESOLVED,
                paymentServices, memberFrank, leadElena, now.minus(2, ChronoUnit.DAYS));

        createItem("Audit double-spend prevention guardrails in wallet transfer API",
                "Validate row-level locking on wallet balance ledger during concurrent debits.",
                WorkItemType.PAYMENT, WorkItemPriority.URGENT, WorkItemStatus.OPEN,
                paymentServices, null, leadElena, now.plus(2, ChronoUnit.DAYS));

        // Compliance & Risk Items
        createItem("Quarterly SOC2 Type II evidence collection",
                "Collect access review logs, change management approvals, and firewall audit trails.",
                WorkItemType.COMPLIANCE, WorkItemPriority.HIGH, WorkItemStatus.IN_PROGRESS,
                complianceRisk, memberHarry, leadGrace, now.plus(10, ChronoUnit.DAYS));

        createItem("GDPR Article 17 Data Subject Erasure request #4092",
                "Process automated purge across customer data warehouse and transactional stores.",
                WorkItemType.COMPLIANCE, WorkItemPriority.HIGH, WorkItemStatus.AWAITING_APPROVAL,
                complianceRisk, memberHarry, memberHarry, now.plus(3, ChronoUnit.DAYS));

        createItem("Annual PCI-DSS penetration testing remediation",
                "Patch TLS 1.1 cipher suites on legacy payment edge proxies.",
                WorkItemType.COMPLIANCE, WorkItemPriority.URGENT, WorkItemStatus.OPEN,
                complianceRisk, null, leadGrace, now.plus(5, ChronoUnit.DAYS));

        createItem("Update employee acceptable use security policy",
                "Include guidelines for AI assistant tools and code generation plugins.",
                WorkItemType.COMPLIANCE, WorkItemPriority.LOW, WorkItemStatus.RESOLVED,
                complianceRisk, memberHarry, leadGrace, now.minus(3, ChronoUnit.DAYS));

        createItem("Review vendor risk assessment for cloud logging provider",
                "Evaluate DPA agreement and sub-processor certifications.",
                WorkItemType.COMPLIANCE, WorkItemPriority.MEDIUM, WorkItemStatus.OPEN,
                complianceRisk, null, leadGrace, now.plus(15, ChronoUnit.DAYS));

        // Customer Support Items
        createItem("High-priority customer unable to export transaction history",
                "Enterprise customer EnterpriseCorp (ID 881) encounters 500 error when exporting CSV.",
                WorkItemType.CUSTOMER_ISSUE, WorkItemPriority.URGENT, WorkItemStatus.IN_PROGRESS,
                customerSupport, memberJack, memberJack, now.plus(1, ChronoUnit.DAYS));

        createItem("Clarify invoice billing address format for EU customers",
                "VAT registration numbers require two-letter country prefix validation.",
                WorkItemType.CUSTOMER_ISSUE, WorkItemPriority.MEDIUM, WorkItemStatus.OPEN,
                customerSupport, null, memberJack, now.plus(4, ChronoUnit.DAYS));

        createItem("OAuth2 SSO login failure for Okta enterprise users",
                "SAML assertion attribute mapping mismatch following recent IdP certificate rotation.",
                WorkItemType.CUSTOMER_ISSUE, WorkItemPriority.HIGH, WorkItemStatus.OPEN,
                customerSupport, null, memberJack, now.plus(2, ChronoUnit.DAYS));

        createItem("Password reset email links expired prematurely",
                "Token TTL was inadvertently configured to 5 minutes instead of 60 minutes.",
                WorkItemType.CUSTOMER_ISSUE, WorkItemPriority.MEDIUM, WorkItemStatus.RESOLVED,
                customerSupport, memberJack, leadBob, now.minus(1, ChronoUnit.DAYS));

        createItem("User session termination on IP address change",
                "Users on mobile networks switching Wi-Fi to cellular get logged out instantly.",
                WorkItemType.CUSTOMER_ISSUE, WorkItemPriority.LOW, WorkItemStatus.OPEN,
                customerSupport, null, memberJack, now.plus(8, ChronoUnit.DAYS));

        log.info("Demo data seeding completed successfully! Seeded 10 users, 4 teams, and 23 work items.");
    }

    private User createUser(String username, String displayName, String email, String passwordHash) {
        User user = new User(UUID.randomUUID(), username, displayName, email, passwordHash);
        return userRepository.save(user);
    }

    private Team createTeam(String name, String description) {
        Team team = new Team(UUID.randomUUID(), name, description);
        return teamRepository.save(team);
    }

    private void createMembership(User user, Team team, TeamRole role) {
        TeamMembership tm = new TeamMembership(UUID.randomUUID(), user, team, role);
        teamMembershipRepository.save(tm);
    }

    private void createItem(String title, String description, WorkItemType type,
                            WorkItemPriority priority, WorkItemStatus status,
                            Team team, User assignee, User creator, Instant dueDate) {
        WorkItem item = new WorkItem(
                UUID.randomUUID(), title, description, type, priority, status,
                team, assignee, creator, dueDate
        );
        WorkItem saved = workItemRepository.save(item);

        // Add creation activity event
        ActivityEvent createdEvent = new ActivityEvent(
                UUID.randomUUID(), saved.getId(), creator.getId(), creator.getDisplayName(),
                ActivityEventType.CREATED, null, saved.getStatus().name(), "Created work item"
        );
        activityEventRepository.save(createdEvent);

        if (assignee != null) {
            ActivityEvent assignedEvent = new ActivityEvent(
                    UUID.randomUUID(), saved.getId(), creator.getId(), creator.getDisplayName(),
                    ActivityEventType.ASSIGNED, "Unassigned", assignee.getDisplayName(),
                    "Assigned to " + assignee.getDisplayName()
            );
            activityEventRepository.save(assignedEvent);
        }

        if (status == WorkItemStatus.AWAITING_APPROVAL) {
            ActivityEvent awaitingEvent = new ActivityEvent(
                    UUID.randomUUID(), saved.getId(), creator.getId(), creator.getDisplayName(),
                    ActivityEventType.STATUS_CHANGED, WorkItemStatus.IN_PROGRESS.name(),
                    WorkItemStatus.AWAITING_APPROVAL.name(), "Submitted for approval"
            );
            activityEventRepository.save(awaitingEvent);

            Comment comment = new Comment(
                    UUID.randomUUID(), saved.getId(), creator.getId(), creator.getDisplayName(),
                    "All changes have been validated on staging. Ready for Lead approval."
            );
            commentRepository.save(comment);
        } else if (status == WorkItemStatus.RESOLVED) {
            Comment comment = new Comment(
                    UUID.randomUUID(), saved.getId(), creator.getId(), creator.getDisplayName(),
                    "Resolution deployed and confirmed operational in production."
            );
            commentRepository.save(comment);
        }
    }
}
