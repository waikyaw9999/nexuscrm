package com.nexuscrm.domain.deal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals")
public class DealController {
    private final DealService deals;

    public DealController(DealService deals) {
        this.deals = deals;
    }

    @GetMapping
    public List<DealView> list(@RequestParam(required = false) String q) {
        return deals.list(q).stream().map(DealView::from).toList();
    }

    @GetMapping("/{id}")
    public DealView get(@PathVariable UUID id) {
        return DealView.from(deals.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealView create(@Valid @RequestBody Body body) {
        return DealView.from(deals.create(body.toUpsert()));
    }

    @PutMapping("/{id}")
    public DealView update(@PathVariable UUID id, @RequestBody Body body) {
        return DealView.from(deals.update(id, body.toUpsert()));
    }

    @PatchMapping("/{id}/stage")
    public DealView stage(@PathVariable UUID id, @RequestBody StageBody body) {
        return DealView.from(deals.move(id, body.stage()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deals.delete(id);
    }

    public record Body(
            @NotBlank String title,
            Deal.Stage stage,
            BigDecimal amount,
            String currency,
            UUID companyId,
            UUID contactId,
            UUID ownerUserId,
            LocalDate expectedCloseDate,
            String notes) {
        DealService.Upsert toUpsert() {
            return new DealService.Upsert(title, stage, amount, currency, companyId, contactId, ownerUserId,
                    expectedCloseDate, notes);
        }
    }

    public record StageBody(Deal.Stage stage) {}

    public record DealView(
            UUID id,
            String title,
            Deal.Stage stage,
            BigDecimal amount,
            String currency,
            UUID companyId,
            UUID contactId,
            UUID ownerUserId,
            LocalDate expectedCloseDate,
            String notes,
            Instant updatedAt) {
        static DealView from(Deal d) {
            return new DealView(d.getId(), d.getTitle(), d.getStage(), d.getAmount(), d.getCurrency(),
                    d.getCompanyId(), d.getContactId(), d.getOwnerUserId(), d.getExpectedCloseDate(),
                    d.getNotes(), d.getUpdatedAt());
        }
    }
}
