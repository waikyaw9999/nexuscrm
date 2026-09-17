package com.nexuscrm.domain.mail;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MailboxProvider {
    List<String> listFolders(Mailbox mailbox);

    List<MailMessage> listMessages(Mailbox mailbox, String folder);

    MailMessage getMessage(Mailbox mailbox, UUID messageId);

    List<MailMessage> listDrafts(Mailbox mailbox);

    MailMessage getDraft(Mailbox mailbox, UUID draftId);

    MailMessage createDraft(Mailbox mailbox, ComposeRequest request);

    MailMessage send(Mailbox mailbox, ComposeRequest request);

    int sync(Mailbox mailbox);

    record ComposeRequest(
            String to,
            String cc,
            String subject,
            String body,
            UUID contactId,
            UUID dealId,
            UUID draftId) {}

    record FolderCount(String name, int count) {}
}
