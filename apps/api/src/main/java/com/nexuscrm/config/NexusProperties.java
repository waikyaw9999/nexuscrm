package com.nexuscrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus")
public class NexusProperties {
    private Jwt jwt = new Jwt();
    private String encryptionKey;
    private String bffApiKey;
    private String bffOrigin;
    private boolean seed = true;
    private Google google = new Google();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getBffApiKey() {
        return bffApiKey;
    }

    public void setBffApiKey(String bffApiKey) {
        this.bffApiKey = bffApiKey;
    }

    public String getBffOrigin() {
        return bffOrigin;
    }

    public void setBffOrigin(String bffOrigin) {
        this.bffOrigin = bffOrigin;
    }

    public boolean isSeed() {
        return seed;
    }

    public void setSeed(boolean seed) {
        this.seed = seed;
    }

    public Google getGoogle() {
        return google;
    }

    public void setGoogle(Google google) {
        this.google = google;
    }

    public static class Jwt {
        private String secret;
        private Duration accessTtl = Duration.ofMinutes(15);
        private Duration refreshTtl = Duration.ofDays(14);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTtl() {
            return accessTtl;
        }

        public void setAccessTtl(Duration accessTtl) {
            this.accessTtl = accessTtl;
        }

        public Duration getRefreshTtl() {
            return refreshTtl;
        }

        public void setRefreshTtl(Duration refreshTtl) {
            this.refreshTtl = refreshTtl;
        }
    }

    public static class Google {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
