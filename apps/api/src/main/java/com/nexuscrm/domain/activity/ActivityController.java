package com.nexuscrm.domain.activity;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class ActivityService {
    private final ActivityRepository activities;
    private final AuditService auditService;

    ActivityService(ActivityRepository activities, AuditService auditService) {
        this.activities = activities;
        this.auditService = auditService;
    }

    List<Activity> list() {
        return activities.findByTenantIdAndDeletedAtIsNullOrderByDueAtAsc(TenantContext.requireTenantId());
    }

    Activity get(UUID id) {
        return activities.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Activity not found"));
    }

    @Transactional
    Activity create(Activity activity) {
        activity.setId(UUID.randomUUID());
        activity.setTenantId(TenantContext.requireTenantId());
        if (activity.getOwnerUserId() == null) {
            activity.setOwnerUserId(TenantContext.userId());
        }
        activities.save(activity);
        auditService.record(activity.getTenantId(), TenantContext.userId(), "activity.created", "activity",
                activity.getId(), activity.getSubject());
        return activity;
    }

    @Transactional
    Activity complete(UUID id) {
        Activity activity = get(id);
        activity.setCompletedAt(Instant.now());
        return activity;
    }

    @Transactional
    void delete(UUID id) {
        get(id).setDeletedAt(Instant.now());
    }
}

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityService activities;

    public ActivityController(ActivityService activities) {
        this.activities = activities;
    }

    @GetMapping
    public List<View> list() {
        return activities.list().stream().map(View::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public View create(@RequestBody Body body) {
        Activity a = new Activity();
        a.setType(body.type());
        a.setSubject(body.subject());
        a.setBody(body.body());
        a.setDueAt(body.dueAt());
        a.setContactId(body.contactId());
        a.setCompanyId(body.companyId());
        a.setDealId(body.dealId());
        return View.from(activities.create(a));
    }

    @PatchMapping("/{id}/complete")
    public View complete(@PathVariable UUID id) {
        return View.from(activities.complete(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        activities.delete(id);
    }

    public record Body(
            Activity.Type type,
            String subject,
            String body,
            Instant dueAt,
            UUID contactId,
            UUID companyId,
            UUID dealId) {}

    public record View(
            UUID id,
            Activity.Type type,
            String subject,
            String body,
            Instant dueAt,
            Instant completedAt,
            UUID contactId,
            UUID companyId,
            UUID dealId,
            UUID ownerUserId) {
        static View from(Activity a) {
            return new View(a.getId(), a.getType(), a.getSubject(), a.getBody(), a.getDueAt(), a.getCompletedAt(),
                    a.getContactId(), a.getCompanyId(), a.getDealId(), a.getOwnerUserId());
        }
    }
}
