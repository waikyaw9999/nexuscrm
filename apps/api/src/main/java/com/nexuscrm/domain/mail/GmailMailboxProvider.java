package com.nexuscrm.domain.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexuscrm.common.ApiException;
import com.nexuscrm.config.NexusProperties;
import com.nexuscrm.crypto.EncryptionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GmailMailboxProvider implements MailboxProvider {
    private final EncryptionService encryption;
    private final ObjectMapper mapper;
    private final NexusProperties properties;
    private final MailMessageRepository messages;
    private final RestClient restClient = RestClient.create();

    public GmailMailboxProvider(
            EncryptionService encryption,
            ObjectMapper mapper,
            NexusProperties properties,
            MailMessageRepository messages) {
        this.encryption = encryption;
        this.mapper = mapper;
        this.properties = properties;
        this.messages = messages;
    }

    @Override
    public List<String> listFolders(Mailbox mailbox) {
        return List.of("INBOX", "SENT", "DRAFTS");
    }

    @Override
    public List<MailMessage> listMessages(Mailbox mailbox, String folder) {
        ensureConfigured();
        String access = accessToken(mailbox);
        String q = switch (folder.toUpperCase()) {
            case "SENT" -> "in:sent";
            case "DRAFTS" -> "in:drafts";
            default -> "in:inbox";
        };
        JsonNode list = restClient.get()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages?maxResults=20&q={q}", q)
                .header("Authorization", "Bearer " + access)
                .retrieve()
                .body(JsonNode.class);
        List<MailMessage> out = new ArrayList<>();
        if (list == null || !list.has("messages")) {
            return out;
        }
        for (JsonNode node : list.get("messages")) {
            String id = node.get("id").asText();
            JsonNode full = restClient.get()
                    .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/{id}?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=To", id)
                    .header("Authorization", "Bearer " + access)
                    .retrieve()
                    .body(JsonNode.class);
            MailMessage msg = new MailMessage();
            msg.setId(UUID.randomUUID());
            msg.setTenantId(mailbox.getTenantId());
            msg.setMailboxId(mailbox.getId());
            msg.setFolder(folder.toUpperCase());
            msg.setProviderMessageId(id);
            msg.setSubject(header(full, "Subject"));
            msg.setFromAddress(header(full, "From"));
            msg.setToAddresses(header(full, "To"));
            msg.setBodyText(full != null && full.has("snippet") ? full.get("snippet").asText() : "");
            out.add(msg);
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
        draft.setSubject(request.subject());
        draft.setToAddresses(request.to());
        draft.setBodyText(request.body());
        return messages.save(draft);
    }

    @Override
    public MailMessage send(Mailbox mailbox, ComposeRequest request) {
        ensureConfigured();
        String access = accessToken(mailbox);
        String raw = """
                To: %s
                Subject: %s
                Content-Type: text/plain; charset=UTF-8

                %s
                """.formatted(request.to(), request.subject(), request.body() == null ? "" : request.body());
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        restClient.post()
                .uri("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                .header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("raw", encoded))
                .retrieve()
                .toBodilessEntity();
        MailMessage sent = new MailMessage();
        sent.setId(UUID.randomUUID());
        sent.setTenantId(mailbox.getTenantId());
        sent.setMailboxId(mailbox.getId());
        sent.setFolder("SENT");
        sent.setSubject(request.subject());
        sent.setToAddresses(request.to());
        sent.setFromAddress(mailbox.getEmailAddress());
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

    public void storeTokens(Mailbox mailbox, String refreshToken, String accessToken) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "refreshToken", refreshToken == null ? "" : refreshToken,
                    "accessToken", accessToken == null ? "" : accessToken));
            mailbox.setEncryptedCredentials(encryption.encrypt(json));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store Gmail tokens", e);
        }
    }

    private void ensureConfigured() {
        if (properties.getGoogle().getClientId() == null || properties.getGoogle().getClientId().isBlank()) {
            throw ApiException.badRequest("Gmail OAuth is not configured. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }
    }

    private String accessToken(Mailbox mailbox) {
        try {
            String json = encryption.decrypt(mailbox.getEncryptedCredentials());
            JsonNode node = mapper.readTree(json);
            String refresh = node.path("refreshToken").asText("");
            if (refresh.isBlank()) {
                return node.path("accessToken").asText();
            }
            JsonNode token = restClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token"
                            .formatted(
                                    properties.getGoogle().getClientId(),
                                    properties.getGoogle().getClientSecret(),
                                    refresh))
                    .retrieve()
                    .body(JsonNode.class);
            return token.get("access_token").asText();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("Gmail mailbox is not connected or tokens are invalid");
        }
    }

    private static String header(JsonNode full, String name) {
        if (full == null || !full.has("payload")) {
            return "";
        }
        JsonNode headers = full.get("payload").path("headers");
        if (!headers.isArray()) {
            return "";
        }
        for (JsonNode header : headers) {
            if (name.equalsIgnoreCase(header.path("name").asText())) {
                return header.path("value").asText();
            }
        }
        return "";
    }
}
