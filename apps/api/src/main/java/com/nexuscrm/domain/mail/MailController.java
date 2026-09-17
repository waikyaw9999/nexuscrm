package com.nexuscrm.domain.mail;

import com.nexuscrm.common.ApiException;
import com.nexuscrm.config.NexusProperties;
import com.nexuscrm.domain.audit.AuditService;
import com.nexuscrm.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class MailService {
    private final MailboxRepository mailboxes;
    private final MailMessageRepository messages;
    private final MockMailboxProvider mock;
    private final GmailMailboxProvider gmail;
    private final ImapSmtpMailboxProvider imap;
    private final AuditService auditService;
    private final NexusProperties properties;

    MailService(
            MailboxRepository mailboxes,
            MailMessageRepository messages,
            MockMailboxProvider mock,
            GmailMailboxProvider gmail,
            ImapSmtpMailboxProvider imap,
            AuditService auditService,
            NexusProperties properties) {
        this.mailboxes = mailboxes;
        this.messages = messages;
        this.mock = mock;
        this.gmail = gmail;
        this.imap = imap;
        this.auditService = auditService;
        this.properties = properties;
    }

    List<Mailbox> listMailboxes() {
        return mailboxes.findByTenantIdAndDeletedAtIsNull(TenantContext.requireTenantId());
    }

    Mailbox getMailbox(UUID id) {
        return mailboxes.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Mailbox not found"));
    }

    @Transactional
    Mailbox connectMock() {
        Mailbox box = new Mailbox();
        box.setId(UUID.randomUUID());
        box.setTenantId(TenantContext.requireTenantId());
        box.setUserId(TenantContext.userId());
        box.setProvider(Mailbox.Provider.MOCK);
        box.setEmailAddress("demo@" + TenantContext.requireTenantId().toString().substring(0, 8) + ".nexuscrm.test");
        box.setDisplayName("Demo mailbox");
        box.setConnectedAt(Instant.now());
        mailboxes.save(box);
        seedSample(box);
        auditService.record(box.getTenantId(), TenantContext.userId(), "mailbox.connected", "mailbox", box.getId(), "MOCK");
        return box;
    }

    @Transactional
    Mailbox connectGmail(String email, String refreshToken, String accessToken) {
        Mailbox box = new Mailbox();
        box.setId(UUID.randomUUID());
        box.setTenantId(TenantContext.requireTenantId());
        box.setUserId(TenantContext.userId());
        box.setProvider(Mailbox.Provider.GMAIL);
        box.setEmailAddress(email);
        box.setDisplayName(email);
        box.setConnectedAt(Instant.now());
        gmail.storeTokens(box, refreshToken, accessToken);
        mailboxes.save(box);
        auditService.record(box.getTenantId(), TenantContext.userId(), "mailbox.connected", "mailbox", box.getId(), "GMAIL");
        return box;
    }

    @Transactional
    Mailbox connectImap(ImapBody body) {
        Mailbox box = new Mailbox();
        box.setId(UUID.randomUUID());
        box.setTenantId(TenantContext.requireTenantId());
        box.setUserId(TenantContext.userId());
        box.setProvider(Mailbox.Provider.IMAP_SMTP);
        box.setEmailAddress(body.email());
        box.setDisplayName(body.email());
        box.setConnectedAt(Instant.now());
        imap.storeCredentials(box, body.imapHost(), body.imapPort(), body.smtpHost(), body.smtpPort(),
                body.username(), body.password());
        mailboxes.save(box);
        auditService.record(box.getTenantId(), TenantContext.userId(), "mailbox.connected", "mailbox", box.getId(), "IMAP");
        return box;
    }

    List<MailMessage> listMessages(UUID mailboxId, String folder) {
        Mailbox box = getMailbox(mailboxId);
        return provider(box).listMessages(box, folder == null ? "INBOX" : folder);
    }

    MailMessage getMessage(UUID mailboxId, UUID messageId) {
        return provider(getMailbox(mailboxId)).getMessage(getMailbox(mailboxId), messageId);
    }

    MailMessage createDraft(UUID mailboxId, MailboxProvider.ComposeRequest request) {
        Mailbox box = getMailbox(mailboxId);
        return provider(box).createDraft(box, request);
    }

    MailMessage send(UUID mailboxId, MailboxProvider.ComposeRequest request) {
        Mailbox box = getMailbox(mailboxId);
        MailMessage sent = provider(box).send(box, request);
        auditService.record(box.getTenantId(), TenantContext.userId(), "mail.sent", "mail", sent.getId(), sent.getSubject());
        return sent;
    }

    @Transactional
    int sync(UUID mailboxId) {
        Mailbox box = getMailbox(mailboxId);
        int count = provider(box).sync(box);
        mailboxes.save(box);
        return count;
    }

    MailboxProvider provider(Mailbox box) {
        return switch (box.getProvider()) {
            case MOCK -> mock;
            case GMAIL -> gmail;
            case IMAP_SMTP -> imap;
        };
    }

    public NexusProperties properties() {
        return properties;
    }

    private void seedSample(Mailbox box) {
        add(box, "INBOX", "Priya Shah <emma.t@example.net>", box.getEmailAddress(),
                "Re: Warehouse automation rollout",
                "Alex — the board signed off. Can we lock Q4 kickoff for the Helix line?", Instant.parse("2026-03-12T14:10:00Z"));
        add(box, "INBOX", "Jonah Patel <rachel.c@example.org>", box.getEmailAddress(),
                "Telemetry RFP questions",
                "Need a one-pager on how Nexus would map our existing fleet IDs.", Instant.parse("2026-03-11T09:22:00Z"));
        add(box, "SENT", box.getEmailAddress(), "Priya Shah <emma.t@example.net>",
                "Proposal: Helix vision system",
                "Attached the SOW and pricing. Happy to walk through stages next week.", Instant.parse("2026-03-10T16:40:00Z"));
        MailMessage draft = new MailMessage();
        draft.setId(UUID.randomUUID());
        draft.setTenantId(box.getTenantId());
        draft.setMailboxId(box.getId());
        draft.setFolder("DRAFTS");
        draft.setFromAddress(box.getEmailAddress());
        draft.setToAddresses("nina.v@example.com");
        draft.setSubject("Follow-up: spare parts portal");
        draft.setBodyText("Hi Elena — checking in on go-live training dates for the portal.");
        messages.save(draft);
    }

    private void add(Mailbox box, String folder, String from, String to, String subject, String body, Instant sentAt) {
        MailMessage msg = new MailMessage();
        msg.setId(UUID.randomUUID());
        msg.setTenantId(box.getTenantId());
        msg.setMailboxId(box.getId());
        msg.setFolder(folder);
        msg.setFromAddress(from);
        msg.setToAddresses(to);
        msg.setSubject(subject);
        msg.setBodyText(body);
        msg.setSentAt(sentAt);
        messages.save(msg);
    }

    record ImapBody(String email, String imapHost, int imapPort, String smtpHost, int smtpPort, String username, String password) {}
}

@RestController
@RequestMapping("/api/v1")
public class MailController {
    private final MailService mail;

    public MailController(MailService mail) {
        this.mail = mail;
    }

    @GetMapping("/mailboxes")
    public List<MailboxView> mailboxes() {
        return mail.listMailboxes().stream().map(MailboxView::from).toList();
    }

    @PostMapping("/mailboxes/mock")
    @ResponseStatus(HttpStatus.CREATED)
    public MailboxView mock() {
        return MailboxView.from(mail.connectMock());
    }

    @PostMapping("/mailboxes/gmail")
    @ResponseStatus(HttpStatus.CREATED)
    public MailboxView gmail(@RequestBody GmailBody body) {
        return MailboxView.from(mail.connectGmail(body.email(), body.refreshToken(), body.accessToken()));
    }

    @PostMapping("/mailboxes/imap")
    @ResponseStatus(HttpStatus.CREATED)
    public MailboxView imap(@RequestBody MailService.ImapBody body) {
        return MailboxView.from(mail.connectImap(body));
    }

    @GetMapping("/mailboxes/{id}/messages")
    public List<MessageView> messages(@PathVariable UUID id, @RequestParam(defaultValue = "INBOX") String folder) {
        return mail.listMessages(id, folder).stream().map(MessageView::from).toList();
    }

    @GetMapping("/mailboxes/{id}/messages/{messageId}")
    public MessageView message(@PathVariable UUID id, @PathVariable UUID messageId) {
        return MessageView.from(mail.getMessage(id, messageId));
    }

    @PostMapping("/mailboxes/{id}/drafts")
    public MessageView draft(@PathVariable UUID id, @RequestBody ComposeBody body) {
        return MessageView.from(mail.createDraft(id, body.toRequest()));
    }

    @PostMapping("/mailboxes/{id}/send")
    public MessageView send(@PathVariable UUID id, @RequestBody ComposeBody body) {
        return MessageView.from(mail.send(id, body.toRequest()));
    }

    @PostMapping("/mailboxes/{id}/sync")
    public SyncView sync(@PathVariable UUID id) {
        return new SyncView(mail.sync(id));
    }

    @GetMapping("/mail/oauth/gmail/config")
    public GmailConfig gmailConfig() {
        var g = mail.properties().getGoogle();
        return new GmailConfig(
                g.getClientId() != null && !g.getClientId().isBlank(),
                g.getRedirectUri());
    }

    public record GmailBody(String email, String refreshToken, String accessToken) {}

    public record ComposeBody(String to, String cc, String subject, String body, UUID contactId, UUID dealId, UUID draftId) {
        MailboxProvider.ComposeRequest toRequest() {
            return new MailboxProvider.ComposeRequest(to, cc, subject, body, contactId, dealId, draftId);
        }
    }

    public record MailboxView(UUID id, String provider, String emailAddress, String displayName, Instant lastSyncAt) {
        static MailboxView from(Mailbox m) {
            return new MailboxView(m.getId(), m.getProvider().name(), m.getEmailAddress(), m.getDisplayName(), m.getLastSyncAt());
        }
    }

    public record MessageView(
            UUID id, UUID mailboxId, String folder, String subject, String fromAddress, String toAddresses,
            String ccAddresses, String bodyText, Instant sentAt, UUID contactId, UUID dealId) {
        static MessageView from(MailMessage m) {
            return new MessageView(m.getId(), m.getMailboxId(), m.getFolder(), m.getSubject(), m.getFromAddress(),
                    m.getToAddresses(), m.getCcAddresses(), m.getBodyText(), m.getSentAt(), m.getContactId(), m.getDealId());
        }
    }

    public record SyncView(int count) {}

    public record GmailConfig(boolean configured, String redirectUri) {}
}
