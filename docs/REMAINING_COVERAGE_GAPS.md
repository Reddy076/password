# Remaining Coverage Gaps Analysis

**Generated:** 2026-02-16
**Current Coverage:** ~70.23%
**Target Coverage:** >80% (SonarCloud Standard)
**Gap to Close:** ~10%

## Executive Summary
After the first round of test improvements (ImportService, ThirdPartyImportService, DtoCoverage), the coverage has jumped from **41% to 70%**. However, significant gaps remain preventing the project from reaching the 80% quality gate.

## Critical Gaps (Logic)
These services contain business logic that is either partially or completely untested.

| Class | Missed Instructions | Notes |
|---|---|---|
| **`VaultService`** | 239 | Still has significant logic gaps (edge cases, complex queries). |
| **`HealthService`** | 195 | Completely untested. Simple service but adds up. |
| **`AuthenticationService`** | 157 | Partial coverage. Likely missing failure scenarios. |
| **`DuressService`** | 126 | Security feature, critical to test. |

## Unexpected Gaps (Models/DTOs)
Despite adding a bulk `DtoCoverageTest`, the following classes still show high missed instruction counts. This suggests the bulk test scanner might be skipping them (e.g., due to inner classes, package scanning issues, or Lombok interactions).

| Class | Missed Instructions | Potential Cause |
|---|---|---|
| `VaultEntry` | 275 | Entity with complex Builder/Relationship logic? |
| `VaultEntryDetailResponse` | 275 | DTO with many fields. |
| `User` | 243 | Core Entity. |
| `VaultEntryResponse` | 199 | DTO. |
| `UserSession` | 164 | Entity. |
| `VaultEntryRequest` | 161 | DTO. |
| `LoginAttempt` | 157 | Entity. |
| `UserSettings` | 142 | Entity. |

## Action Plan
To reach >80% coverage:

1.  **Fix the DTO Scanner**: Investigate why `DtoCoverageTest` is not effectively covering `VaultEntry` and `User`. Fixing this single test could boost coverage by another 5-10%.
2.  **Test `HealthService`**: Create a simple test for this service.
3.  **Finish `VaultService`**: Add tests for the remaining edge cases.
