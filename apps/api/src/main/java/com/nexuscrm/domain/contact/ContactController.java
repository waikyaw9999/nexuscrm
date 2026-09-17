package com.nexuscrm.domain.contact;

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
@RequestMapping("/api/v1/contacts")
public class ContactController {
    private final ContactService contacts;

    public ContactController(ContactService contacts) {
        this.contacts = contacts;
    }

    @GetMapping
    public PageResponse<View> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = contacts.list(q, page, size);
        return new PageResponse<>(result.items().stream().map(View::from).toList(), result.total(), page, size);
    }

    @GetMapping("/{id}")
    public View get(@PathVariable UUID id) {
        return View.from(contacts.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public View create(@Valid @RequestBody Body body) {
        return View.from(contacts.create(body.toEntity()));
    }

    @PutMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Body body) {
        return View.from(contacts.update(id, body.toEntity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        contacts.delete(id);
    }

    public record Body(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String email,
            String phone,
            String title,
            String tags,
            String notes,
            UUID companyId) {
        Contact toEntity() {
            Contact c = new Contact();
            c.setFirstName(firstName);
            c.setLastName(lastName);
            c.setEmail(email);
            c.setPhone(phone);
            c.setTitle(title);
            c.setTags(tags);
            c.setNotes(notes);
            c.setCompanyId(companyId);
            return c;
        }
    }

    public record View(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String title,
            String tags,
            String notes,
            UUID companyId,
            Instant updatedAt) {
        static View from(Contact c) {
            return new View(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(),
                    c.getTitle(), c.getTags(), c.getNotes(), c.getCompanyId(), c.getUpdatedAt());
        }
    }
}
