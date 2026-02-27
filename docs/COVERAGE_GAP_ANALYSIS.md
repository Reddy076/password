# Coverage Gap Analysis

**Generated:** 2026-02-16
**Overall Coverage:** ~41.4%
**Target Coverage:** >80% (SonarCloud Standard)

## Executive Summary
The current test suite covers approximately 41% of the codebase, which is significantly below the SonarCloud quality gate. The primary deficits are in **Data Import Services** and **Data Models/DTOs**. Core services like `VaultService` also have significant untested paths.

### What are "Instructions"?
In JaCoCo coverage reports, **"Instructions"** refer to **Java Bytecode Instructions**.
- This is the smallest unit of execution for the Java Virtual Machine (JVM).
- Unlike "Line Coverage" (which just tells you if a line was touched), "Instruction Coverage" tells you if every part of a complex line was executed.
- **Example:** A single line of code like `return isUserValid() ? "Success" : "Failure";` might contain multiple bytecode instructions (checking the condition, branching, loading strings, returning).
- Using instructions provides a much more precise measure of how much logical code is actually being tested.

## Critical Gaps (Services)
These classes contain business logic but have low coverage. They should be the highest priority for new tests.

| Class | Missed Instructions | Covered Instructions | Coverage % | Recommendation |
|---|---|---|---|---|
| **`VaultService`** | 499 | 539 | ~52% | **High Priority**. Core logic. Extensive edge cases needed. |
| **`ImportService`** | 336 | 16 | ~4.5% | **Critical**. Almost entirely untested. Needs integration tests. |
| **`ThirdPartyImportService`** | 311 | 16 | ~4.9% | **Critical**. Almost entirely untested. |
| `AuthenticationService` | 157 | 435 | ~73% | Medium. Review specific missing auth flows. |
| `DuressService` | 126 | 80 | ~39% | Medium. Security feature, needs higher assurance. |
| `NotificationService` | 105 | 133 | ~56% | Medium. |

## Significant Gaps (Models & DTOs)
These classes account for a large volume of "missed" instructions, mostly due to untested getters/setters/builders/constructors.

| Class | Missed Instructions | Notes |
|---|---|---|
| `VaultEntryDetailResponse` | 592 | Complex DTO |
| `VaultEntry` | 571 | Core Entity |
| `User` | 498 | Core Entity |
| `VaultEntryResponse` | 444 | DTO |
| `UserSession` | 380 | Entity |
| `LoginAttempt` | 354 | Entity |
| `UserSettings` | 331 | Entity |
| `VaultEntryRequest` | 329 | Request DTO |

**DTO Recommendation:** 
SonarCloud typically excludes simple POJOs (Getters/Setters) if configured, or you can add simple "Pojo Tests" (e.g., using OpenPojo library) to quickly bump coverage for these files without significant effort.

## Other Gaps
- `OtpToken`: 286 missed
- `TwoFactorAuth`: 285 missed
- `SecurityAlert`: 315 missed
- `BackupExport`: 270 missed
- `Folder`: 279 missed

## Action Plan
1.  **Immediate**: Write unit tests for `ImportService` and `ThirdPartyImportService`.
2.  **Secondary**: Expand `VaultService` tests to cover the remaining 48% of logic.
3.  **Tertiary**: Add a generic POJO tester to cover all DTOs and Entities, likely boosting overall coverage by 20-30%.
