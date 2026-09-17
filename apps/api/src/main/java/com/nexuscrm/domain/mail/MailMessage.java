package com.nexuscrm.domain.mail;

import com.nexuscrm.tenant.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mail_messages")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MailMessage extends TenantScopedEntity {
    @Id
    private UUID id;

    @Column(name = "mailbox_id", nullable = false)
    private UUID mailboxId;

    @Column(nullable = false)
    private String folder;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    private String subject;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_addresses", columnDefinition = "text")
    private String toAddresses;

    @Column(name = "cc_addresses", columnDefinition = "text")
    private String ccAddresses;

    @Column(name = "body_text", columnDefinition = "text")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "text")
    private String bodyHtml;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "deal_id")
    private UUID dealId;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMailboxId() {
        return mailboxId;
    }

    public void setMailboxId(UUID mailboxId) {
        this.mailboxId = mailboxId;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddresses() {
        return toAddresses;
    }

    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }

    public String getCcAddresses() {
        return ccAddresses;
    }

    public void setCcAddresses(String ccAddresses) {
        this.ccAddresses = ccAddresses;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public UUID getContactId() {
        return contactId;
    }

    public void setContactId(UUID contactId) {
        this.contactId = contactId;
    }

    public UUID getDealId() {
        return dealId;
    }

    public void setDealId(UUID dealId) {
        this.dealId = dealId;
    }
}
