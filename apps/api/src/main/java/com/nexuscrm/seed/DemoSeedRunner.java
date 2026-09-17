package com.nexuscrm.seed;

import com.nexuscrm.config.NexusProperties;
import com.nexuscrm.domain.activity.Activity;
import com.nexuscrm.domain.activity.ActivityRepository;
import com.nexuscrm.domain.company.Company;
import com.nexuscrm.domain.company.CompanyRepository;
import com.nexuscrm.domain.contact.Contact;
import com.nexuscrm.domain.contact.ContactRepository;
import com.nexuscrm.domain.deal.Deal;
import com.nexuscrm.domain.deal.DealRepository;
import com.nexuscrm.domain.mail.MailMessage;
import com.nexuscrm.domain.mail.MailMessageRepository;
import com.nexuscrm.domain.mail.Mailbox;
import com.nexuscrm.domain.mail.MailboxRepository;
import com.nexuscrm.domain.membership.Membership;
import com.nexuscrm.domain.membership.MembershipRepository;
import com.nexuscrm.domain.tenant.Tenant;
import com.nexuscrm.domain.tenant.TenantRepository;
import com.nexuscrm.domain.user.AppUser;
import com.nexuscrm.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class DemoSeedRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    public static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final String DEMO_EMAIL = "oscar.d@example.net";

    private final NexusProperties properties;
    private final UserRepository users;
    private final TenantRepository tenants;
    private final MembershipRepository memberships;
    private final CompanyRepository companies;
    private final ContactRepository contacts;
    private final DealRepository deals;
    private final ActivityRepository activities;
    private final MailboxRepository mailboxes;
    private final MailMessageRepository messages;
    private final PasswordEncoder passwordEncoder;

    public DemoSeedRunner(
            NexusProperties properties,
            UserRepository users,
            TenantRepository tenants,
            MembershipRepository memberships,
            CompanyRepository companies,
            ContactRepository contacts,
            DealRepository deals,
            ActivityRepository activities,
            MailboxRepository mailboxes,
            MailMessageRepository messages,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
        this.companies = companies;
        this.contacts = contacts;
        this.deals = deals;
        this.activities = activities;
        this.mailboxes = mailboxes;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isSeed()) {
            return;
        }
        if (users.findByEmailIgnoreCase(DEMO_EMAIL).isPresent()) {
            log.info("Demo tenant already present ({})", DEMO_EMAIL);
            return;
        }
        log.info("Seeding Acme Robotics demo workspace");

        Tenant tenant = new Tenant(TENANT_ID, "Acme Robotics", "acme-robotics");
        tenant.setPlan(Tenant.Plan.PRO);
        tenant.setLogoUrl(null);
        tenants.save(tenant);

        AppUser user = new AppUser(USER_ID, DEMO_EMAIL, passwordEncoder.encode("Demo1234!"), "Alex Rivera");
        users.save(user);
        memberships.save(new Membership(UUID.randomUUID(), TENANT_ID, USER_ID, Membership.Role.OWNER));

        Company helix = company("Helix Manufacturing", "helixmfg.com", "Industrial automation", "https://helixmfg.com");
        Company northwind = company("Northwind Logistics", "northwindlog.com", "Fleet & logistics", "https://northwindlog.com");
        Company atlas = company("Atlas Foods", "atlasfoods.co", "Food production", "https://atlasfoods.co");
        Company meridian = company("Meridian Energy", "meridian.energy", "Utilities", "https://meridian.energy");

        Contact priya = contact("Priya", "Shah", "emma.t@example.net", "CTO", helix.getId(), "decision-maker,champion");
        Contact jonah = contact("Jonah", "Patel", "rachel.c@example.org", "VP Operations", northwind.getId(), "economic-buyer");
        Contact elena = contact("Elena", "Rossi", "nina.v@example.com", "Procurement Lead", atlas.getId(), "procurement");
        Contact nora = contact("Nora", "Kim", "ursula.b@example.com", "Plant Manager", helix.getId(), "influencer");
        contact("Marcus", "Cole", "hannah.h@example.com", "Head of Digital", meridian.getId(), "new-logo");

        Deal warehouse = deal("Warehouse automation", Deal.Stage.QUALIFIED, "180000", helix.getId(), priya.getId(), LocalDate.of(2026, 6, 30));
        Deal vision = deal("Assembly line vision", Deal.Stage.PROPOSAL, "92000", helix.getId(), nora.getId(), LocalDate.of(2026, 5, 15));
        Deal telemetry = deal("Fleet telemetry", Deal.Stage.NEGOTIATION, "240000", northwind.getId(), jonah.getId(), LocalDate.of(2026, 4, 20));
        Deal portal = deal("Spare parts portal", Deal.Stage.WON, "45000", atlas.getId(), elena.getId(), LocalDate.of(2026, 2, 28));
        deal("Safety audit software", Deal.Stage.LEAD, "28000", meridian.getId(), null, LocalDate.of(2026, 8, 1));

        activity(Activity.Type.CALL, "Discovery call with Priya", Instant.parse("2026-03-18T16:00:00Z"), priya.getId(), helix.getId(), warehouse.getId());
        activity(Activity.Type.MEETING, "Vision system demo", Instant.parse("2026-03-20T18:30:00Z"), nora.getId(), helix.getId(), vision.getId());
        activity(Activity.Type.TASK, "Send revised telemetry SOW", Instant.parse("2026-03-16T21:00:00Z"), jonah.getId(), northwind.getId(), telemetry.getId());
        Activity done = activity(Activity.Type.NOTE, "Portal go-live recap", Instant.parse("2026-03-01T15:00:00Z"), elena.getId(), atlas.getId(), portal.getId());
        done.setCompletedAt(Instant.parse("2026-03-01T16:00:00Z"));
        activities.save(done);

        Mailbox box = new Mailbox();
        box.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        box.setTenantId(TENANT_ID);
        box.setUserId(USER_ID);
        box.setProvider(Mailbox.Provider.MOCK);
        box.setEmailAddress(DEMO_EMAIL);
        box.setDisplayName("Alex Rivera");
        box.setConnectedAt(Instant.parse("2026-02-01T12:00:00Z"));
        box.setLastSyncAt(Instant.parse("2026-03-12T14:15:00Z"));
        mailboxes.save(box);

        mail(box, "INBOX", "Priya Shah <emma.t@example.net>", DEMO_EMAIL,
                "Re: Warehouse automation rollout",
                "Alex — the board signed off. Can we lock Q4 kickoff for the Helix line?",
                Instant.parse("2026-03-12T14:10:00Z"), priya.getId(), warehouse.getId());
        mail(box, "INBOX", "Jonah Patel <rachel.c@example.org>", DEMO_EMAIL,
                "Telemetry RFP questions",
                "Need a one-pager on how Nexus would map our existing fleet IDs before legal reviews the MSA.",
                Instant.parse("2026-03-11T09:22:00Z"), jonah.getId(), telemetry.getId());
        mail(box, "SENT", DEMO_EMAIL, "Priya Shah <emma.t@example.net>",
                "Proposal: Helix vision system",
                "Attached the SOW and pricing. Happy to walk through stages next week.",
                Instant.parse("2026-03-10T16:40:00Z"), priya.getId(), vision.getId());
        MailMessage draft = new MailMessage();
        draft.setId(UUID.randomUUID());
        draft.setTenantId(TENANT_ID);
        draft.setMailboxId(box.getId());
        draft.setFolder("DRAFTS");
        draft.setFromAddress(DEMO_EMAIL);
        draft.setToAddresses("nina.v@example.com");
        draft.setSubject("Follow-up: spare parts portal");
        draft.setBodyText("Hi Elena — checking in on go-live training dates for the portal. We can run a 45-minute session for the warehouse team.");
        draft.setContactId(elena.getId());
        draft.setDealId(portal.getId());
        messages.save(draft);

        log.info("Seeded demo login {} / Demo1234!", DEMO_EMAIL);
    }

    private Company company(String name, String domain, String industry, String website) {
        Company c = new Company();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT_ID);
        c.setName(name);
        c.setDomain(domain);
        c.setIndustry(industry);
        c.setWebsite(website);
        return companies.save(c);
    }

    private Contact contact(String first, String last, String email, String title, UUID companyId, String tags) {
        Contact c = new Contact();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT_ID);
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setTitle(title);
        c.setCompanyId(companyId);
        c.setTags(tags);
        return contacts.save(c);
    }

    private Deal deal(String title, Deal.Stage stage, String amount, UUID companyId, UUID contactId, LocalDate close) {
        Deal d = new Deal();
        d.setId(UUID.randomUUID());
        d.setTenantId(TENANT_ID);
        d.setTitle(title);
        d.setStage(stage);
        d.setAmount(new BigDecimal(amount));
        d.setCurrency("USD");
        d.setCompanyId(companyId);
        d.setContactId(contactId);
        d.setOwnerUserId(USER_ID);
        d.setExpectedCloseDate(close);
        return deals.save(d);
    }

    private Activity activity(Activity.Type type, String subject, Instant due, UUID contactId, UUID companyId, UUID dealId) {
        Activity a = new Activity();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT_ID);
        a.setType(type);
        a.setSubject(subject);
        a.setDueAt(due);
        a.setContactId(contactId);
        a.setCompanyId(companyId);
        a.setDealId(dealId);
        a.setOwnerUserId(USER_ID);
        return activities.save(a);
    }

    private void mail(Mailbox box, String folder, String from, String to, String subject, String body, Instant sent,
                      UUID contactId, UUID dealId) {
        MailMessage msg = new MailMessage();
        msg.setId(UUID.randomUUID());
        msg.setTenantId(TENANT_ID);
        msg.setMailboxId(box.getId());
        msg.setFolder(folder);
        msg.setFromAddress(from);
        msg.setToAddresses(to);
        msg.setSubject(subject);
        msg.setBodyText(body);
        msg.setSentAt(sent);
        msg.setContactId(contactId);
        msg.setDealId(dealId);
        messages.save(msg);
    }
}
