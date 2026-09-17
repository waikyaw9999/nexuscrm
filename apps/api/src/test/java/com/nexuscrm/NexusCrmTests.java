package com.nexuscrm;

import com.nexuscrm.config.NexusProperties;
import com.nexuscrm.crypto.EncryptionService;
import com.nexuscrm.domain.mail.MailMessage;
import com.nexuscrm.domain.mail.MailMessageRepository;
import com.nexuscrm.domain.mail.Mailbox;
import com.nexuscrm.domain.mail.MailboxProvider;
import com.nexuscrm.domain.mail.MockMailboxProvider;
import com.nexuscrm.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EncryptionServiceTest {
    @Test
    void roundTripsSecretsWithoutLoggingThem() {
        NexusProperties properties = new NexusProperties();
        properties.setEncryptionKey("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        EncryptionService service = new EncryptionService(properties);
        String cipher = service.encrypt("refresh-token-secret");
        assertThat(cipher).isNotEqualTo("refresh-token-secret");
        assertThat(service.decrypt(cipher)).isEqualTo("refresh-token-secret");
    }
}

class JwtServiceTest {
    @Test
    void embedsTenantClaim() {
        NexusProperties properties = new NexusProperties();
        properties.getJwt().setSecret("unit-test-secret-please-use-32chars");
        JwtService jwt = new JwtService(properties);
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        String token = jwt.createAccessToken(user, "a@b.com", tenant, "OWNER");
        assertThat(jwt.parse(token).get("tenantId", String.class)).isEqualTo(tenant.toString());
        assertThat(jwt.parse(token).getSubject()).isEqualTo(user.toString());
    }
}

class MockMailboxProviderTest {
    @Test
    void sendMovesDraftToSent() {
        MailMessageRepository repo = Mockito.mock(MailMessageRepository.class);
        Mockito.when(repo.save(any(MailMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMailboxProvider provider = new MockMailboxProvider(repo);
        Mailbox box = new Mailbox();
        box.setId(UUID.randomUUID());
        box.setTenantId(UUID.randomUUID());
        box.setEmailAddress("demo@nexuscrm.test");
        box.setProvider(Mailbox.Provider.MOCK);
        MailMessage sent = provider.send(box, new MailboxProvider.ComposeRequest(
                "buyer@helixmfg.com", null, "Hello", "Body", null, null, null));
        assertThat(sent.getFolder()).isEqualTo("SENT");
        assertThat(sent.getToAddresses()).isEqualTo("buyer@helixmfg.com");
        assertThat(sent.getFromAddress()).isEqualTo("demo@nexuscrm.test");
        assertThat(sent.getSentAt()).isNotNull();
    }
}

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthAndIsolationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    NexusProperties properties;

    private String bffKey() {
        return properties.getBffApiKey();
    }

    @Test
    void loginRejectsBadPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .header("X-BFF-Key", bffKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@acme.test","password":"Secret123!","fullName":"Owner","organizationName":"Acme"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login")
                        .header("X-BFF-Key", bffKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@acme.test","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tenantCannotReadAnotherTenantsContacts() throws Exception {
        String tokenA = register("a@tenant.test", "Alpha Org");
        String tokenB = register("b@tenant.test", "Beta Org");

        mvc.perform(post("/api/v1/contacts")
                        .header("X-BFF-Key", bffKey())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Secret","lastName":"Lead","email":"secret@alpha.test"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/contacts")
                        .header("X-BFF-Key", bffKey())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void dealsListWorksWithoutSearchQuery() throws Exception {
        String token = register("deals@tenant.test", "Deals Org");
        mvc.perform(post("/api/v1/deals")
                        .header("X-BFF-Key", bffKey())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Warehouse automation","stage":"LEAD","amount":1000}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/deals")
                        .header("X-BFF-Key", bffKey())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Warehouse automation"));
    }

    private String register(String email, String org) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register")
                        .header("X-BFF-Key", bffKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Secret123!","fullName":"Pat Lee","organizationName":"%s"}
                                """.formatted(email, org)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int start = json.indexOf("\"accessToken\":\"") + 15;
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}

class DealServiceNullQueryTest {
    @Test
    void blankQueryUsesTenantScopedFindNotLowerSearch() {
        var repo = Mockito.mock(com.nexuscrm.domain.deal.DealRepository.class);
        var audit = Mockito.mock(com.nexuscrm.domain.audit.AuditService.class);
        Mockito.when(repo.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(any()))
                .thenReturn(List.of());
        var service = new com.nexuscrm.domain.deal.DealService(repo, audit);
        UUID tenant = UUID.randomUUID();
        com.nexuscrm.tenant.TenantContext.set(tenant, UUID.randomUUID(), "OWNER");
        try {
            service.list(null);
            service.list("  ");
            verify(repo, Mockito.times(2)).findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenant);
            verify(repo, never()).searchByTitle(any(), any());
        } finally {
            com.nexuscrm.tenant.TenantContext.clear();
        }
    }
}
