package com.nexuscrm.domain.dashboard;

import com.nexuscrm.domain.activity.Activity;
import com.nexuscrm.domain.activity.ActivityRepository;
import com.nexuscrm.domain.contact.ContactRepository;
import com.nexuscrm.domain.deal.Deal;
import com.nexuscrm.domain.deal.DealRepository;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final ContactRepository contacts;
    private final DealRepository deals;
    private final ActivityRepository activities;

    public DashboardController(
            ContactRepository contacts, DealRepository deals, ActivityRepository activities) {
        this.contacts = contacts;
        this.deals = deals;
        this.activities = activities;
    }

    @GetMapping
    public DashboardView get() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Deal> openDeals = deals.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantId);
        BigDecimal pipeline = openDeals.stream()
                .filter(d -> d.getStage() != Deal.Stage.WON && d.getStage() != Deal.Stage.LOST)
                .map(d -> d.getAmount() == null ? BigDecimal.ZERO : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal won = openDeals.stream()
                .filter(d -> d.getStage() == Deal.Stage.WON)
                .map(d -> d.getAmount() == null ? BigDecimal.ZERO : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<StageCount> stages = Arrays.stream(Deal.Stage.values())
                .map(stage -> new StageCount(stage.name(),
                        openDeals.stream().filter(d -> d.getStage() == stage).count()))
                .toList();
        List<ActivityView> recent = activities.findByTenantIdAndDeletedAtIsNullOrderByDueAtAsc(tenantId).stream()
                .limit(8)
                .map(ActivityView::from)
                .toList();
        return new DashboardView(
                contacts.countByTenantIdAndDeletedAtIsNull(tenantId),
                deals.countByTenantIdAndDeletedAtIsNull(tenantId),
                activities.countByTenantIdAndDeletedAtIsNullAndCompletedAtIsNull(tenantId),
                pipeline,
                won,
                stages,
                recent);
    }

    public record DashboardView(
            long contacts,
            long deals,
            long openActivities,
            BigDecimal pipelineValue,
            BigDecimal wonValue,
            List<StageCount> stages,
            List<ActivityView> recentActivities) {}

    public record StageCount(String stage, long count) {}

    public record ActivityView(UUID id, String type, String subject, Instant dueAt, Instant completedAt) {
        static ActivityView from(Activity a) {
            return new ActivityView(a.getId(), a.getType().name(), a.getSubject(), a.getDueAt(), a.getCompletedAt());
        }
    }
}
