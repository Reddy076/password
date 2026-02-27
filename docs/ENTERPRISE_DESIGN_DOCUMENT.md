# 🏢 Rev-PasswordManager Enterprise Design Document

## Document Version: 1.0
## Status: Planning Phase
## Prerequisites: All 32 base features completed

---

# 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Phase 9: Multi-Tenancy](#phase-9-multi-tenancy)
4. [Phase 10: Role-Based Access Control](#phase-10-role-based-access-control)
5. [Phase 11: Enterprise SSO](#phase-11-enterprise-sso)
6. [Phase 12: API Platform](#phase-12-api-platform)
7. [Phase 13: Compliance & Audit](#phase-13-compliance--audit)
8. [Phase 14: Billing & Licensing](#phase-14-billing--licensing)
9. [Phase 15: Infrastructure & Scaling](#phase-15-infrastructure--scaling)
10. [Phase 16: Advanced Security](#phase-16-advanced-security)
11. [Database Schema Changes](#database-schema-changes)
12. [API Endpoints Summary](#api-endpoints-summary)
13. [Implementation Timeline](#implementation-timeline)

---

# Executive Summary

This document outlines the design for upgrading Rev-PasswordManager from a **personal password manager** (32 features) to an **enterprise-grade solution** (50+ features).

| Metric | Current (32 Features) | Enterprise Target |
|--------|----------------------|-------------------|
| Files | ~127 | 200+ |
| Features | 32 | 50+ |
| Database Tables | 18 | 35+ |
| API Endpoints | ~40 | 100+ |
| Target Users | Individual | Organizations (1000+ users) |

---

# Architecture Overview

## Current Architecture (Personal)
```
┌─────────────────────────────────────────────┐
│                   Client                     │
│            (Angular Frontend)                │
└─────────────────────┬───────────────────────┘
                      │ REST API
┌─────────────────────▼───────────────────────┐
│              Spring Boot API                 │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐       │
│  │  Auth   │ │  Vault  │ │ Security│       │
│  │ Service │ │ Service │ │ Service │       │
│  └────┬────┘ └────┬────┘ └────┬────┘       │
└───────┼───────────┼───────────┼─────────────┘
        │           │           │
┌───────▼───────────▼───────────▼─────────────┐
│              MySQL Database                  │
│          (Single Instance)                   │
└─────────────────────────────────────────────┘
```

## Enterprise Architecture (Target)
```
┌─────────────────────────────────────────────────────────────────┐
│                         Clients                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  Web App │ │ Mobile   │ │ Desktop  │ │ Browser  │           │
│  │ (Angular)│ │ (iOS/And)│ │(Electron)│ │Extensions│           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
┌───────▼────────────▼────────────▼────────────▼──────────────────┐
│                    API Gateway / Load Balancer                   │
│                   (Rate Limiting, SSL Termination)               │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                    Spring Boot API Cluster                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Instance │ │ Instance │ │ Instance │ │ Instance │           │
│  │    1     │ │    2     │ │    3     │ │    N     │           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘           │
└───────┼────────────┼────────────┼────────────┼──────────────────┘
        │            │            │            │
   ┌────▼────┐  ┌────▼────┐  ┌────▼────┐ ┌────▼────┐
   │  Redis  │  │ RabbitMQ│  │ MySQL   │ │ MySQL   │
   │ (Cache) │  │ (Queue) │  │ Primary │ │ Replica │
   └─────────┘  └─────────┘  └─────────┘ └─────────┘
```

---

# Phase 9: Multi-Tenancy

## 9.1 Overview

Convert single-user application to support multiple organizations with isolated data.

## 9.2 New Entities

### Organization.java
```java
@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;  // URL-friendly identifier
    
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;  // FREE, PRO, ENTERPRISE
    
    private Integer maxUsers;
    private Integer maxVaultEntries;
    
    @OneToMany(mappedBy = "organization")
    private List<OrganizationUser> members;
    
    @OneToMany(mappedBy = "organization")
    private List<Team> teams;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### OrganizationUser.java
```java
@Entity
@Table(name = "organization_users")
public class OrganizationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private OrganizationRole role;  // OWNER, ADMIN, MEMBER, GUEST
    
    @ManyToOne
    @JoinColumn(name = "invited_by")
    private User invitedBy;
    
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
}
```

### Team.java
```java
@Entity
@Table(name = "teams")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    private String name;
    private String description;
    
    @OneToMany(mappedBy = "team")
    private List<TeamMember> members;
    
    @OneToMany(mappedBy = "team")
    private List<SharedVault> sharedVaults;
}
```

### SharedVault.java
```java
@Entity
@Table(name = "shared_vaults")
public class SharedVault {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;  // Nullable - can be org-wide
    
    private String name;
    private String description;
    
    @OneToMany(mappedBy = "sharedVault")
    private List<SharedVaultEntry> entries;
    
    @OneToMany(mappedBy = "sharedVault")
    private List<SharedVaultPermission> permissions;
}
```

## 9.3 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `Organization.java` | `model/organization/` | Organization entity |
| `OrganizationUser.java` | `model/organization/` | User-org mapping |
| `Team.java` | `model/organization/` | Team entity |
| `TeamMember.java` | `model/organization/` | User-team mapping |
| `SharedVault.java` | `model/organization/` | Shared vault entity |
| `SharedVaultEntry.java` | `model/organization/` | Entries in shared vault |
| `SharedVaultPermission.java` | `model/organization/` | Access permissions |
| `OrganizationRepository.java` | `repository/` | Org DB operations |
| `TeamRepository.java` | `repository/` | Team DB operations |
| `SharedVaultRepository.java` | `repository/` | Shared vault DB ops |
| `OrganizationService.java` | `service/organization/` | Org business logic |
| `TeamService.java` | `service/organization/` | Team business logic |
| `SharedVaultService.java` | `service/organization/` | Shared vault logic |
| `InvitationService.java` | `service/organization/` | User invitations |
| `OrganizationController.java` | `controller/` | Org endpoints |
| `TeamController.java` | `controller/` | Team endpoints |
| `SharedVaultController.java` | `controller/` | Shared vault endpoints |

## 9.4 Modifications to Existing Files

| File | Changes |
|------|---------|
| `User.java` | Add `currentOrganization`, `organizations` relationships |
| `VaultEntry.java` | Add optional `sharedVault` reference |
| `JwtTokenProvider.java` | Include `organizationId` in JWT claims |
| `SecurityConfig.java` | Add organization context filter |

## 9.5 API Endpoints

```
POST   /api/organizations                    # Create organization
GET    /api/organizations/{id}               # Get organization details
PUT    /api/organizations/{id}               # Update organization
DELETE /api/organizations/{id}               # Delete organization

POST   /api/organizations/{id}/invite        # Invite user
GET    /api/organizations/{id}/members       # List members
DELETE /api/organizations/{id}/members/{uid} # Remove member

POST   /api/organizations/{id}/teams         # Create team
GET    /api/organizations/{id}/teams         # List teams
PUT    /api/teams/{id}                       # Update team
DELETE /api/teams/{id}                       # Delete team

POST   /api/shared-vaults                    # Create shared vault
GET    /api/shared-vaults                    # List accessible shared vaults
GET    /api/shared-vaults/{id}/entries       # Get entries in shared vault
POST   /api/shared-vaults/{id}/entries       # Add entry to shared vault
```

---

# Phase 10: Role-Based Access Control

## 10.1 Overview

Implement granular permissions system for organization and resource access.

## 10.2 Permission Model

### Role Hierarchy
```
SUPER_ADMIN (System-wide)
    └── ORG_OWNER
            └── ORG_ADMIN
                    └── MANAGER
                            └── MEMBER
                                    └── GUEST
```

### Permission Categories
```java
public enum Permission {
    // Vault Permissions
    VAULT_CREATE,
    VAULT_READ,
    VAULT_UPDATE,
    VAULT_DELETE,
    VAULT_SHARE,
    
    // User Management
    USER_INVITE,
    USER_REMOVE,
    USER_ROLE_ASSIGN,
    
    // Team Management
    TEAM_CREATE,
    TEAM_MANAGE,
    TEAM_DELETE,
    
    // Organization
    ORG_SETTINGS,
    ORG_BILLING,
    ORG_AUDIT_VIEW,
    ORG_SSO_MANAGE,
    
    // System
    SYSTEM_ADMIN,
    API_KEY_MANAGE
}
```

## 10.3 New Entities

### Role.java
```java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;  // Null for system roles
    
    private boolean isCustom;
    
    @ManyToMany
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions;
}
```

### PermissionEntity.java
```java
@Entity
@Table(name = "permissions")
public class PermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private Permission permission;
    
    private String description;
    private String category;
}
```

## 10.4 Security Annotations

```java
// Controller method protection
@PreAuthorize("hasPermission(#orgId, 'Organization', 'ORG_SETTINGS')")
public ResponseEntity<?> updateOrgSettings(@PathVariable Long orgId, ...) { }

@PreAuthorize("hasRole('ORG_ADMIN') or hasPermission(#vaultId, 'SharedVault', 'VAULT_SHARE')")
public ResponseEntity<?> shareVault(@PathVariable Long vaultId, ...) { }
```

## 10.5 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `Role.java` | `model/security/` | Role entity |
| `PermissionEntity.java` | `model/security/` | Permission entity |
| `Permission.java` | `model/security/` | Permission enum |
| `UserRole.java` | `model/security/` | User-role mapping |
| `RoleRepository.java` | `repository/` | Role DB operations |
| `PermissionRepository.java` | `repository/` | Permission DB ops |
| `RoleService.java` | `service/security/` | Role management |
| `PermissionEvaluator.java` | `security/` | Custom permission logic |
| `RoleController.java` | `controller/` | Role endpoints |

---

# Phase 11: Enterprise SSO

## 11.1 Overview

Support enterprise identity providers for single sign-on.

## 11.2 Supported Protocols

| Protocol | Use Case |
|----------|----------|
| SAML 2.0 | Enterprise IdPs (Okta, Azure AD, OneLogin) |
| OAuth 2.0 / OIDC | Modern IdPs (Google, Microsoft) |
| LDAP | On-premise Active Directory |

## 11.3 SAML Configuration

### SamlIdentityProvider.java
```java
@Entity
@Table(name = "saml_identity_providers")
public class SamlIdentityProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    private String entityId;
    private String ssoUrl;
    private String sloUrl;
    
    @Lob
    private String certificate;
    
    private String nameIdFormat;
    
    @ElementCollection
    private Map<String, String> attributeMappings;
    
    private boolean enabled;
    private boolean jitProvisioningEnabled;
}
```

## 11.4 SSO Flow

```
┌──────────┐     ┌──────────────┐     ┌─────────────┐
│   User   │     │ Password Mgr │     │     IdP     │
│ Browser  │     │   (SP)       │     │ (Okta/Azure)│
└────┬─────┘     └──────┬───────┘     └──────┬──────┘
     │                  │                    │
     │ 1. Access /login │                    │
     ├─────────────────>│                    │
     │                  │                    │
     │ 2. Redirect to IdP                    │
     │<─────────────────┤                    │
     │                  │                    │
     │ 3. Login at IdP  │                    │
     ├───────────────────────────────────────>
     │                  │                    │
     │ 4. SAML Response │                    │
     │<───────────────────────────────────────
     │                  │                    │
     │ 5. POST SAML to SP                    │
     ├─────────────────>│                    │
     │                  │                    │
     │ 6. Validate & Create Session          │
     │                  ├───────────────────>│
     │                  │                    │
     │ 7. JWT Token     │                    │
     │<─────────────────┤                    │
```

## 11.5 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `SamlIdentityProvider.java` | `model/sso/` | SAML IdP config entity |
| `OAuthProvider.java` | `model/sso/` | OAuth IdP config entity |
| `LdapConfig.java` | `model/sso/` | LDAP config entity |
| `SamlConfig.java` | `config/` | Spring SAML setup |
| `OAuth2Config.java` | `config/` | OAuth2 setup |
| `SamlAuthProvider.java` | `security/sso/` | SAML authentication |
| `SamlUserDetailsService.java` | `security/sso/` | User creation from SAML |
| `LdapAuthProvider.java` | `security/sso/` | LDAP authentication |
| `SsoService.java` | `service/sso/` | SSO business logic |
| `JitProvisioningService.java` | `service/sso/` | Just-in-time user creation |
| `SsoController.java` | `controller/` | SSO endpoints |

## 11.6 API Endpoints

```
GET    /api/sso/saml/metadata                 # SP metadata XML
POST   /api/sso/saml/acs                      # Assertion Consumer Service
GET    /api/sso/saml/login                    # Initiate SAML login
POST   /api/sso/saml/logout                   # SAML logout

GET    /api/oauth2/authorize/{provider}       # OAuth2 login
GET    /api/oauth2/callback/{provider}        # OAuth2 callback

POST   /api/organizations/{id}/sso/saml       # Configure SAML
POST   /api/organizations/{id}/sso/oauth      # Configure OAuth
POST   /api/organizations/{id}/sso/ldap       # Configure LDAP
GET    /api/organizations/{id}/sso/test       # Test SSO config
```

---

# Phase 12: API Platform

## 12.1 Overview

Provide public API with API keys, webhooks, and versioning.

## 12.2 API Key Management

### ApiKey.java
```java
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    private String name;
    private String keyHash;  // Store hashed, show prefix only
    private String keyPrefix;  // e.g., "rpm_live_"
    
    @ElementCollection
    private Set<String> scopes;  // vault:read, vault:write, etc.
    
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private boolean isActive;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

## 12.3 Webhook System

### Webhook.java
```java
@Entity
@Table(name = "webhooks")
public class Webhook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    private String url;
    private String secret;  // For HMAC signature
    
    @ElementCollection
    @Enumerated(EnumType.STRING)
    private Set<WebhookEvent> events;
    
    private boolean isActive;
    private int failureCount;
    private LocalDateTime lastTriggeredAt;
}
```

### WebhookEvent.java
```java
public enum WebhookEvent {
    // Vault Events
    ENTRY_CREATED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    ENTRY_ACCESSED,
    
    // User Events
    USER_JOINED,
    USER_LEFT,
    USER_ROLE_CHANGED,
    
    // Security Events
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    MFA_ENABLED,
    SUSPICIOUS_ACTIVITY,
    
    // Organization Events
    TEAM_CREATED,
    SETTINGS_CHANGED
}
```

## 12.4 API Versioning

```
/api/v1/vault/entries     # Current version
/api/v2/vault/entries     # Future version with breaking changes
```

## 12.5 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `ApiKey.java` | `model/api/` | API key entity |
| `ApiKeyRepository.java` | `repository/` | API key DB ops |
| `ApiKeyService.java` | `service/api/` | Key generation/validation |
| `ApiKeyAuthFilter.java` | `security/` | API key auth filter |
| `Webhook.java` | `model/api/` | Webhook config entity |
| `WebhookEvent.java` | `model/api/` | Event enum |
| `WebhookDelivery.java` | `model/api/` | Delivery log entity |
| `WebhookService.java` | `service/api/` | Webhook dispatch |
| `WebhookController.java` | `controller/` | Webhook endpoints |
| `ApiKeyController.java` | `controller/` | API key endpoints |

## 12.6 API Endpoints

```
POST   /api/api-keys                          # Create API key
GET    /api/api-keys                          # List API keys
DELETE /api/api-keys/{id}                     # Revoke API key
POST   /api/api-keys/{id}/rotate              # Rotate key

POST   /api/webhooks                          # Create webhook
GET    /api/webhooks                          # List webhooks
PUT    /api/webhooks/{id}                     # Update webhook
DELETE /api/webhooks/{id}                     # Delete webhook
GET    /api/webhooks/{id}/deliveries          # View delivery log
POST   /api/webhooks/{id}/test                # Test webhook
```

---

# Phase 13: Compliance & Audit

## 13.1 Overview

Enhanced audit logging and compliance reporting for regulated industries.

## 13.2 Compliance Features

| Framework | Requirements |
|-----------|-------------|
| **SOC 2** | Access controls, audit logging, encryption |
| **GDPR** | Data export, right to erasure, consent tracking |
| **HIPAA** | Access audit, encryption, BAA support |
| **ISO 27001** | Security controls, risk management |

## 13.3 Enhanced Audit Log

### EnterpriseAuditLog.java
```java
@Entity
@Table(name = "enterprise_audit_logs")
public class EnterpriseAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    
    @Column(columnDefinition = "JSON")
    private String beforeState;  // JSON snapshot before change
    
    @Column(columnDefinition = "JSON")
    private String afterState;   // JSON snapshot after change
    
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL
    
    @CreationTimestamp
    private LocalDateTime timestamp;
}
```

## 13.4 GDPR Data Export

### GdprExportService.java
```java
@Service
public class GdprExportService {
    
    public byte[] exportUserData(Long userId) {
        // Collect all user data from all tables
        // Package as ZIP with JSON files
        // Include: profile, vault entries, audit logs, settings
    }
    
    public void processErasureRequest(Long userId, String verificationCode) {
        // Verify user identity
        // Anonymize audit logs
        // Delete all personal data
        // Generate erasure certificate
    }
}
```

## 13.5 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `EnterpriseAuditLog.java` | `model/compliance/` | Enhanced audit entity |
| `ComplianceReport.java` | `model/compliance/` | Report entity |
| `DataRetentionPolicy.java` | `model/compliance/` | Retention rules |
| `GdprExportService.java` | `service/compliance/` | GDPR data export |
| `GdprErasureService.java` | `service/compliance/` | Right to erasure |
| `ComplianceReportService.java` | `service/compliance/` | Report generation |
| `AuditExportService.java` | `service/compliance/` | Export to SIEM |
| `ComplianceController.java` | `controller/` | Compliance endpoints |

## 13.6 API Endpoints

```
GET    /api/compliance/audit-logs             # Query audit logs
POST   /api/compliance/audit-logs/export      # Export to CSV/JSON
GET    /api/compliance/reports/soc2           # Generate SOC2 report
GET    /api/compliance/reports/gdpr           # Generate GDPR report

POST   /api/gdpr/export                       # Request data export
POST   /api/gdpr/erasure                      # Request account erasure
GET    /api/gdpr/consent                      # View consent status
PUT    /api/gdpr/consent                      # Update consent
```

---

# Phase 14: Billing & Licensing

## 14.1 Overview

Subscription management and payment processing.

## 14.2 Subscription Plans

| Plan | Users | Features |
|------|-------|----------|
| **Free** | 1 | Personal vault, 50 entries |
| **Pro** | 1 | Unlimited entries, sharing, 2FA |
| **Team** | Up to 10 | Shared vaults, teams |
| **Business** | Up to 100 | SSO, API, audit logs |
| **Enterprise** | Unlimited | Custom, dedicated support |

## 14.3 Entities

### Subscription.java
```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;  // ACTIVE, PAST_DUE, CANCELED
    
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    
    private Integer quantity;  // Number of seats
}
```

## 14.4 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `Subscription.java` | `model/billing/` | Subscription entity |
| `Invoice.java` | `model/billing/` | Invoice entity |
| `PaymentMethod.java` | `model/billing/` | Payment method entity |
| `UsageRecord.java` | `model/billing/` | Usage tracking |
| `SubscriptionService.java` | `service/billing/` | Subscription logic |
| `StripeService.java` | `service/billing/` | Stripe integration |
| `InvoiceService.java` | `service/billing/` | Invoice generation |
| `BillingController.java` | `controller/` | Billing endpoints |
| `StripeWebhookController.java` | `controller/` | Stripe webhooks |

---

# Phase 15: Infrastructure & Scaling

## 15.1 Caching Layer (Redis)

### CacheConfig.java
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("users",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("vaultEntries",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(1)))
            .build();
    }
}
```

## 15.2 Message Queue (RabbitMQ)

```java
// Async processing for:
// - Webhook deliveries
// - Email sending
// - Audit log writing
// - Report generation
// - Breach checking

@Service
public class WebhookPublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void publishEvent(WebhookEvent event, Object payload) {
        rabbitTemplate.convertAndSend("webhooks.exchange", 
                                      event.name(), 
                                      payload);
    }
}
```

## 15.3 Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `CacheConfig.java` | `config/` | Redis cache setup |
| `RabbitMQConfig.java` | `config/` | RabbitMQ setup |
| `WebhookConsumer.java` | `messaging/` | Process webhook queue |
| `EmailConsumer.java` | `messaging/` | Process email queue |
| `DistributedLockService.java` | `service/` | Redis-based locking |

---

# Phase 16: Advanced Security

## 16.1 Hardware Key Support (FIDO2/WebAuthn)

```java
@Entity
@Table(name = "security_keys")
public class SecurityKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String credentialId;
    private byte[] publicKey;
    private long signCount;
    
    private String nickname;  // "YubiKey 5", "Touch ID"
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
```

## 16.2 Breach Monitoring

```java
@Service
public class BreachMonitoringService {
    
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    public void checkForBreaches() {
        // For each vault entry
        // Hash password with SHA-1 (k-anonymity)
        // Check against HIBP API
        // Create alert if breached
    }
}
```

## 16.3 Emergency Access

```java
@Entity
@Table(name = "emergency_access_grants")
public class EmergencyAccessGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private User grantor;
    
    @ManyToOne
    private User grantee;
    
    private Integer waitDays;  // Days before access granted
    
    @Enumerated(EnumType.STRING)
    private EmergencyAccessStatus status;  // INVITED, ACCEPTED, REQUESTED, APPROVED
    
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
}
```

---

# Database Schema Changes

## New Tables Summary

```sql
-- Phase 9: Multi-Tenancy
CREATE TABLE organizations (...);
CREATE TABLE organization_users (...);
CREATE TABLE teams (...);
CREATE TABLE team_members (...);
CREATE TABLE shared_vaults (...);
CREATE TABLE shared_vault_entries (...);
CREATE TABLE shared_vault_permissions (...);

-- Phase 10: RBAC
CREATE TABLE roles (...);
CREATE TABLE permissions (...);
CREATE TABLE role_permissions (...);
CREATE TABLE user_roles (...);

-- Phase 11: SSO
CREATE TABLE saml_identity_providers (...);
CREATE TABLE oauth_providers (...);
CREATE TABLE ldap_configs (...);

-- Phase 12: API Platform
CREATE TABLE api_keys (...);
CREATE TABLE webhooks (...);
CREATE TABLE webhook_events (...);
CREATE TABLE webhook_deliveries (...);

-- Phase 13: Compliance
CREATE TABLE enterprise_audit_logs (...);
CREATE TABLE compliance_reports (...);
CREATE TABLE data_retention_policies (...);
CREATE TABLE gdpr_requests (...);

-- Phase 14: Billing
CREATE TABLE subscriptions (...);
CREATE TABLE invoices (...);
CREATE TABLE payment_methods (...);
CREATE TABLE usage_records (...);

-- Phase 16: Advanced Security
CREATE TABLE security_keys (...);
CREATE TABLE emergency_access_grants (...);
CREATE TABLE breach_alerts (...);
```

**Total New Tables: 25+**
**Total Tables After Upgrade: 43+**

---

# API Endpoints Summary

| Category | Endpoints | Count |
|----------|-----------|-------|
| Organizations | /api/organizations/* | 12 |
| Teams | /api/teams/* | 8 |
| Shared Vaults | /api/shared-vaults/* | 10 |
| Roles & Permissions | /api/roles/* | 8 |
| SSO | /api/sso/* | 12 |
| API Keys | /api/api-keys/* | 6 |
| Webhooks | /api/webhooks/* | 8 |
| Compliance | /api/compliance/* | 10 |
| GDPR | /api/gdpr/* | 4 |
| Billing | /api/billing/* | 12 |
| **Total New Endpoints** | | **~90** |
| **Total After Upgrade** | | **~130** |

---

# Implementation Timeline

## Recommended Order

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 9: Multi-Tenancy | 4-5 weeks | Base 32 features |
| Phase 10: RBAC | 2-3 weeks | Phase 9 |
| Phase 11: SSO | 3-4 weeks | Phase 9, 10 |
| Phase 12: API Platform | 2-3 weeks | Phase 9, 10 |
| Phase 13: Compliance | 3-4 weeks | Phase 9, 10 |
| Phase 14: Billing | 2-3 weeks | Phase 9 |
| Phase 15: Infrastructure | 2-3 weeks | Any time |
| Phase 16: Advanced Security | 2-3 weeks | Phase 9, 10 |
| **TOTAL** | **20-28 weeks** | |

## Team Composition

| Role | Count | Responsibilities |
|------|-------|------------------|
| Backend Developer | 2-3 | API, services, security |
| Frontend Developer | 1-2 | Admin dashboards, SSO flows |
| DevOps Engineer | 1 | Infrastructure, CI/CD |
| QA Engineer | 1 | Testing, security audit |
| Project Manager | 1 | Timeline, stakeholders |

---

# Summary

## Current State → Enterprise State

| Aspect | Current (32 Features) | Enterprise (50+ Features) |
|--------|----------------------|---------------------------|
| Users | Individual | Organizations (1000+) |
| Auth | Password + 2FA | + SSO (SAML, OAuth, LDAP) |
| Vaults | Personal only | + Shared, Team vaults |
| Access | All or nothing | RBAC with permissions |
| API | Internal only | Public API + Webhooks |
| Compliance | Basic audit | SOC2, GDPR, HIPAA |
| Scale | Single instance | Clustered, cached, queued |
| Security | Strong | + Hardware keys, breach monitoring |

---

> **Next Steps:** Complete all 32 base features, then begin Phase 9 (Multi-Tenancy) as the foundation for all enterprise capabilities.
