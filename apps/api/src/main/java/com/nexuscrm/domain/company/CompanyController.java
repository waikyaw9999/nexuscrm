package com.nexuscrm.domain.company;

import com.nexuscrm.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companies;

    public CompanyController(CompanyService companies) {
        this.companies = companies;
    }

    @GetMapping
    public PageResponse<View> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = companies.list(q, page, size);
        return new PageResponse<>(result.items().stream().map(View::from).toList(), result.total(), page, size);
    }

    @GetMapping("/{id}")
    public View get(@PathVariable UUID id) {
        return View.from(companies.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public View create(@Valid @RequestBody Body body) {
        return View.from(companies.create(body.toEntity()));
    }

    @PutMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Body body) {
        return View.from(companies.update(id, body.toEntity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        companies.delete(id);
    }

    public record Body(
            @NotBlank String name,
            String domain,
            String industry,
            String phone,
            String website,
            String notes) {
        Company toEntity() {
            Company c = new Company();
            c.setName(name);
            c.setDomain(domain);
            c.setIndustry(industry);
            c.setPhone(phone);
            c.setWebsite(website);
            c.setNotes(notes);
            return c;
        }
    }

    public record View(
            UUID id,
            String name,
            String domain,
            String industry,
            String phone,
            String website,
            String notes,
            Instant updatedAt) {
        static View from(Company c) {
            return new View(c.getId(), c.getName(), c.getDomain(), c.getIndustry(), c.getPhone(),
                    c.getWebsite(), c.getNotes(), c.getUpdatedAt());
        }
    }
}
