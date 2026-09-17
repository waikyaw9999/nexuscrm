package com.nexuscrm.domain.mail;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class MockMailboxProvider implements MailboxProvider {
    private final MailMessageRepository messages;

    public MockMailboxProvider(MailMessageRepository messages) {
        this.messages = messages;
    }

    @Override
    public List<String> listFolders(Mailbox mailbox) {
        return List.of("INBOX", "SENT", "DRAFTS");
    }

    @Override
    public List<MailMessage> listMessages(Mailbox mailbox, String folder) {
        return messages.findByTenantIdAndMailboxIdAndFolderOrderBySentAtDesc(
                mailbox.getTenantId(), mailbox.getId(), folder.toUpperCase());
    }

    @Override
    public MailMessage getMessage(Mailbox mailbox, UUID messageId) {
        return messages.findByIdAndTenantId(messageId, mailbox.getTenantId())
                .filter(m -> m.getMailboxId().equals(mailbox.getId()))
                .orElseThrow(() -> ApiException.notFound("Message not found"));
    }

    @Override
    public List<MailMessage> listDrafts(Mailbox mailbox) {
        return listMessages(mailbox, "DRAFTS");
    }

    @Override
    public MailMessage getDraft(Mailbox mailbox, UUID draftId) {
        MailMessage draft = getMessage(mailbox, draftId);
        if (!"DRAFTS".equals(draft.getFolder())) {
            throw ApiException.notFound("Draft not found");
        }
        return draft;
    }

    @Override
    public MailMessage createDraft(Mailbox mailbox, ComposeRequest request) {
        MailMessage msg = fromRequest(mailbox, request);
        msg.setFolder("DRAFTS");
        msg.setSentAt(null);
        return messages.save(msg);
    }

    @Override
    public MailMessage send(Mailbox mailbox, ComposeRequest request) {
        MailMessage msg;
        if (request.draftId() != null) {
            msg = getDraft(mailbox, request.draftId());
            apply(msg, request, mailbox);
        } else {
            msg = fromRequest(mailbox, request);
        }
        msg.setFolder("SENT");
        msg.setFromAddress(mailbox.getEmailAddress());
        msg.setSentAt(Instant.now());
        return messages.save(msg);
    }

    @Override
    public int sync(Mailbox mailbox) {
        mailbox.setLastSyncAt(Instant.now());
        return messages.findByMailboxId(mailbox.getId()).size();
    }

    private MailMessage fromRequest(Mailbox mailbox, ComposeRequest request) {
        MailMessage msg = new MailMessage();
        msg.setId(UUID.randomUUID());
        msg.setTenantId(mailbox.getTenantId() != null ? mailbox.getTenantId() : TenantContext.tenantId());
        msg.setMailboxId(mailbox.getId());
        apply(msg, request, mailbox);
        return msg;
    }

    private void apply(MailMessage msg, ComposeRequest request, Mailbox mailbox) {
        msg.setFromAddress(mailbox.getEmailAddress());
        msg.setToAddresses(request.to());
        msg.setCcAddresses(request.cc());
        msg.setSubject(request.subject());
        msg.setBodyText(request.body());
        msg.setContactId(request.contactId());
        msg.setDealId(request.dealId());
    }
}
