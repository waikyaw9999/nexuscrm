package com.nexuscrm.domain.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuscrm.common.ApiException;
import com.nexuscrm.crypto.EncryptionService;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Component
public class ImapSmtpMailboxProvider implements MailboxProvider {
    private final EncryptionService encryption;
    private final ObjectMapper mapper;
    private final MailMessageRepository messages;

    public ImapSmtpMailboxProvider(
            EncryptionService encryption, ObjectMapper mapper, MailMessageRepository messages) {
        this.encryption = encryption;
        this.mapper = mapper;
        this.messages = messages;
    }

    @Override
    public List<String> listFolders(Mailbox mailbox) {
        return List.of("INBOX", "SENT", "DRAFTS");
    }

    @Override
    public List<MailMessage> listMessages(Mailbox mailbox, String folder) {
        Creds creds = creds(mailbox);
        List<MailMessage> out = new ArrayList<>();
        try {
            Session session = session(creds);
            try (Store store = session.getStore("imaps")) {
                store.connect(creds.imapHost, creds.username, creds.password);
                String name = switch (folder.toUpperCase()) {
                    case "SENT" -> guess(store, "Sent");
                    case "DRAFTS" -> guess(store, "Drafts");
                    default -> "INBOX";
                };
                Folder imapFolder = store.getFolder(name);
                imapFolder.open(Folder.READ_ONLY);
                int end = imapFolder.getMessageCount();
                int start = Math.max(1, end - 19);
                if (end > 0) {
                    for (Message message : imapFolder.getMessages(start, end)) {
                        MailMessage mapped = new MailMessage();
                        mapped.setId(UUID.randomUUID());
                        mapped.setTenantId(mailbox.getTenantId());
                        mapped.setMailboxId(mailbox.getId());
                        mapped.setFolder(folder.toUpperCase());
                        mapped.setSubject(message.getSubject());
                        mapped.setFromAddress(InternetAddress.toString(message.getFrom()));
                        mapped.setToAddresses(InternetAddress.toString(message.getRecipients(Message.RecipientType.TO)));
                        mapped.setBodyText(message.getContent() == null ? "" : message.getContent().toString());
                        if (message.getSentDate() != null) {
                            mapped.setSentAt(message.getSentDate().toInstant());
                        }
                        out.add(mapped);
                    }
                }
                imapFolder.close(false);
            }
        } catch (Exception e) {
            throw ApiException.badRequest("IMAP list failed: " + e.getMessage());
        }
        return out;
    }

    @Override
    public MailMessage getMessage(Mailbox mailbox, UUID messageId) {
        return messages.findByIdAndTenantId(messageId, mailbox.getTenantId())
                .orElseThrow(() -> ApiException.notFound("Message not found"));
    }

    @Override
    public List<MailMessage> listDrafts(Mailbox mailbox) {
        return listMessages(mailbox, "DRAFTS");
    }

    @Override
    public MailMessage getDraft(Mailbox mailbox, UUID draftId) {
        return getMessage(mailbox, draftId);
    }

    @Override
    public MailMessage createDraft(Mailbox mailbox, ComposeRequest request) {
        MailMessage draft = new MailMessage();
        draft.setId(UUID.randomUUID());
        draft.setTenantId(mailbox.getTenantId());
        draft.setMailboxId(mailbox.getId());
        draft.setFolder("DRAFTS");
        draft.setToAddresses(request.to());
        draft.setSubject(request.subject());
        draft.setBodyText(request.body());
        return messages.save(draft);
    }

    @Override
    public MailMessage send(Mailbox mailbox, ComposeRequest request) {
        Creds creds = creds(mailbox);
        try {
            Session session = session(creds);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailbox.getEmailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.to()));
            message.setSubject(request.subject());
            message.setText(request.body() == null ? "" : request.body());
            Transport.send(message);
        } catch (Exception e) {
            throw ApiException.badRequest("SMTP send failed: " + e.getMessage());
        }
        MailMessage sent = new MailMessage();
        sent.setId(UUID.randomUUID());
        sent.setTenantId(mailbox.getTenantId());
        sent.setMailboxId(mailbox.getId());
        sent.setFolder("SENT");
        sent.setFromAddress(mailbox.getEmailAddress());
        sent.setToAddresses(request.to());
        sent.setSubject(request.subject());
        sent.setBodyText(request.body());
        sent.setSentAt(Instant.now());
        sent.setContactId(request.contactId());
        sent.setDealId(request.dealId());
        return messages.save(sent);
    }

    @Override
    public int sync(Mailbox mailbox) {
        mailbox.setLastSyncAt(Instant.now());
        return listMessages(mailbox, "INBOX").size();
    }

    public void storeCredentials(Mailbox mailbox, String imapHost, int imapPort, String smtpHost, int smtpPort,
                                 String username, String password) {
        try {
            String json = mapper.writeValueAsString(java.util.Map.of(
                    "imapHost", imapHost,
                    "imapPort", imapPort,
                    "smtpHost", smtpHost,
                    "smtpPort", smtpPort,
                    "username", username,
                    "password", password));
            mailbox.setEncryptedCredentials(encryption.encrypt(json));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Creds creds(Mailbox mailbox) {
        try {
            JsonNode node = mapper.readTree(encryption.decrypt(mailbox.getEncryptedCredentials()));
            return new Creds(
                    node.path("imapHost").asText("imap.example.com"),
                    node.path("imapPort").asInt(993),
                    node.path("smtpHost").asText("smtp.example.com"),
                    node.path("smtpPort").asInt(587),
                    node.path("username").asText(),
                    node.path("password").asText());
        } catch (Exception e) {
            throw ApiException.badRequest("IMAP/SMTP mailbox is missing credentials");
        }
    }

    private Session session(Creds creds) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", creds.imapHost);
        props.put("mail.imaps.port", String.valueOf(creds.imapPort));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", creds.smtpHost);
        props.put("mail.smtp.port", String.valueOf(creds.smtpPort));
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(creds.username, creds.password);
            }
        });
    }

    private String guess(Store store, String want) throws Exception {
        for (Folder folder : store.getDefaultFolder().list("*")) {
            if (folder.getName().equalsIgnoreCase(want)) {
                return folder.getFullName();
            }
        }
        return want;
    }

    private record Creds(String imapHost, int imapPort, String smtpHost, int smtpPort, String username, String password) {}
}
