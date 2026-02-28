# 🔐 RevaultX — Frontend Development Plan
## Modern SaaS Password Manager · Angular 18+ · Bootstrap 5 · Pill-Shaped Island Design System

> **Stack:** Angular 18 (Standalone Components + Signals) · TypeScript 5 · Bootstrap 5.3 · SCSS · Angular Animations
> **Design Language:** Pill-Shaped **Island** UI System — floating, detached, rounded navigation elements that hover above the background
> **Backend Base URL:** `http://localhost:8080`
> **Responsive:** Mobile-first, all breakpoints (320px → 4K)

---

## 📐 Design System: The Pill Language

### Core Visual Identity
The entire UI is built around **pill-shaped** elements — every button, badge, input, card, and container uses generous border-radius to create a soft, modern, premium feel.

```scss
// Pill Design Tokens
--pill-radius-sm:    20px;   // Tags, badges, small chips
--pill-radius-md:    28px;   // Buttons, inputs, small cards
--pill-radius-lg:    40px;   // Cards, panels, modals
--pill-radius-xl:    60px;   // Hero sections, large containers
--pill-radius-full:  9999px; // Perfect pills (toggle switches, avatars)

// Color Palette — Dark Mode First
--bg-primary:        #0a0f1e;   // Deep navy background
--bg-surface:        #111827;   // Card surfaces
--bg-elevated:       #1a2235;   // Elevated panels
--bg-hover:          #1e2d45;   // Hover states
--accent-primary:    #6366f1;   // Indigo — primary actions
--accent-secondary:  #8b5cf6;   // Purple — secondary
--accent-success:    #10b981;   // Emerald — success/strong
--accent-warning:    #f59e0b;   // Amber — warnings
--accent-danger:     #ef4444;   // Red — danger/weak
--accent-info:       #3b82f6;   // Blue — info
--text-primary:      #f1f5f9;   // Primary text
--text-secondary:    #94a3b8;   // Muted text
--text-muted:        #475569;   // Very muted
--border-subtle:     #1e293b;   // Subtle borders
--border-default:    #334155;   // Default borders
--glow-primary:      rgba(99, 102, 241, 0.3);  // Indigo glow
--glow-success:      rgba(16, 185, 129, 0.3);  // Green glow
--glow-danger:       rgba(239, 68, 68, 0.3);   // Red glow
```

### Animation System
```scss
// Micro-interaction timings
--transition-fast:   150ms ease;
--transition-base:   250ms ease;
--transition-slow:   400ms cubic-bezier(0.4, 0, 0.2, 1);
--spring:            300ms cubic-bezier(0.34, 1.56, 0.64, 1); // Springy
--bounce:            500ms cubic-bezier(0.68, -0.55, 0.265, 1.55);

// Keyframe animations used throughout
@keyframes pill-float    { /* subtle floating for hero elements */ }
@keyframes pill-pulse    { /* pulsing glow for active states */ }
@keyframes slide-up      { /* content entrance from bottom */ }
@keyframes slide-in-right{ /* panel slide from right */ }
@keyframes fade-scale    { /* modal/dropdown entrance */ }
@keyframes shimmer       { /* skeleton loading effect */ }
@keyframes strength-fill { /* password strength bar fill */ }
```

### Bootstrap 5 Customization
Bootstrap 5.3 is used as the grid/utility foundation, fully overridden with the pill design system:
- All `border-radius` variables overridden to pill values
- Custom component variants added via SCSS `@extend`
- Bootstrap grid (12-col) + CSS Grid hybrid layout
- Bootstrap breakpoints: `xs(320)`, `sm(576)`, `md(768)`, `lg(992)`, `xl(1200)`, `xxl(1400)`

---

## 🏝️ Island Design System — Floating Navigation Elements

### What is the Island Design?

The **Island Design** is the defining visual concept of RevaultX's navigation. Instead of navigation bars and sidebars that are flush against the screen edges (like traditional apps), every navigation element — the **sidebar**, the **topbar/navbar**, and the **bottom navigation** — is a **floating, detached "island"** that hovers above the background with visible gaps on all sides.

Think of it like:
- **macOS Dock** — the dock floats above the desktop with space around it
- **iOS Dynamic Island** — a pill-shaped element that floats at the top, detached from the screen edge
- **Raycast / Linear / Vercel dashboards** — floating sidebars with rounded corners and visible background gaps

The result is a **premium, airy, modern SaaS feel** where the background (deep navy `#0a0f1e`) is always visible around the navigation islands, creating depth and separation between UI layers.

---

### Island Design Principles

#### 1. Detachment — Never Touch the Edges
Every navigation island has **margin/padding from the screen edge**. Nothing is flush against the viewport boundary.

```scss
// Island spacing tokens
--island-gap:         12px;   // Gap between island and screen edge (mobile)
--island-gap-md:      16px;   // Gap on tablet
--island-gap-lg:      20px;   // Gap on desktop
--island-gap-xl:      24px;   // Gap on large screens
```

#### 2. Pill Shape — Maximum Roundness
Islands use the largest practical border-radius to appear as pill/capsule shapes:

```scss
// Island border-radius
--island-radius:      24px;   // Sidebar, topbar, bottom nav
--island-radius-sm:   18px;   // Compact islands (mobile bottom nav)
--island-radius-lg:   32px;   // Large floating panels
```

#### 3. Frosted Glass Surface
Islands use a **semi-transparent frosted glass** background with backdrop blur, so the deep navy background subtly shows through:

```scss
// Island surface styles
--island-bg:          rgba(17, 24, 39, 0.85);   // Semi-transparent dark
--island-blur:        blur(20px);                // Backdrop blur
--island-border:      1px solid rgba(255, 255, 255, 0.06); // Subtle border
--island-shadow:      0 8px 32px rgba(0, 0, 0, 0.4),
                      0 2px 8px rgba(0, 0, 0, 0.2);  // Depth shadow
--island-shadow-hover: 0 12px 40px rgba(0, 0, 0, 0.5),
                       0 4px 12px rgba(99, 102, 241, 0.15); // Glow on hover
```

#### 4. Floating Position — `position: fixed` with Inset Margins
Islands are `position: fixed` with explicit inset values so they always float relative to the viewport:

```scss
// Sidebar island positioning
.sidebar-island {
  position: fixed;
  top: var(--island-gap-lg);
  left: var(--island-gap-lg);
  bottom: var(--island-gap-lg);
  width: 240px;
  border-radius: var(--island-radius);
  // ... glass surface styles
}

// Topbar island positioning
.topbar-island {
  position: fixed;
  top: var(--island-gap-lg);
  left: calc(240px + var(--island-gap-lg) * 2);  // After sidebar + gap
  right: var(--island-gap-lg);
  height: 60px;
  border-radius: var(--island-radius);
  // ... glass surface styles
}

// Bottom nav island positioning (mobile only)
.bottom-nav-island {
  position: fixed;
  bottom: var(--island-gap);
  left: var(--island-gap);
  right: var(--island-gap);
  height: 64px;
  border-radius: var(--island-radius);
  // ... glass surface styles
}
```

#### 5. Content Area Offset
The main content area is offset to account for the floating islands, with matching margins:

```scss
.main-content {
  margin-left: calc(240px + var(--island-gap-lg) * 2);  // Sidebar width + gaps
  margin-top: calc(60px + var(--island-gap-lg) * 2);    // Topbar height + gaps
  margin-right: var(--island-gap-lg);
  margin-bottom: var(--island-gap-lg);
  // Content itself is also a rounded island
  border-radius: var(--island-radius);
  background: var(--bg-surface);
  min-height: calc(100vh - 60px - var(--island-gap-lg) * 3);
}
```

---

### Island Visual Layout — Desktop

```
╔═══════════════════════════════════════════════════════════════════╗
║  [deep navy background #0a0f1e — always visible in gaps]         ║
║                                                                   ║
║  ╭──────────╮  ╭─────────────────────────────────────────────╮   ║
║  │          │  │  🔍 Search...          🔔 3   👤 John  ⚡  │   ║
║  │ 🔐       │  ╰─────────────────────────────────────────────╯   ║
║  │ RevaultX │                                                     ║
║  │          │  ╭─────────────────────────────────────────────╮   ║
║  │ ─────── │  │                                             │   ║
║  │ 🏠 Home  │  │         MAIN CONTENT AREA                  │   ║
║  │ 🔐 Vault │  │         (router-outlet)                    │   ║
║  │ 🔑 Gen   │  │                                             │   ║
║  │ 🛡️ Sec   │  │                                             │   ║
║  │ ⚙️ Set   │  │                                             │   ║
║  │          │  │                                             │   ║
║  │ ─────── │  │                                             │   ║
║  │ 👤 John  │  ╰─────────────────────────────────────────────╯   ║
║  │ [Logout] │                                                     ║
║  ╰──────────╯                                                     ║
╚═══════════════════════════════════════════════════════════════════╝
```

**Key observations:**
- The deep navy background is **always visible** in the gaps between islands
- The sidebar island has **equal gaps** on top, left, and bottom from the viewport
- The topbar island has a **gap from the top** and aligns with the sidebar's top edge
- The content area is **also a rounded island** — it's not a raw page, it's a floating panel
- All four corners of every island are **fully rounded** (24px radius)

---

### Island Visual Layout — Tablet (768px–991px)

On tablet, the sidebar collapses to an **icon-only island** (60px wide):

```
╔══════════════════════════════════════════════════════╗
║  [deep navy background]                              ║
║                                                      ║
║  ╭────╮  ╭──────────────────────────────────────╮   ║
║  │    │  │  🔍 Search...          🔔  👤        │   ║
║  │ 🏠 │  ╰──────────────────────────────────────╯   ║
║  │ 🔐 │                                             ║
║  │ 🔑 │  ╭──────────────────────────────────────╮   ║
║  │ 🛡️ │  │                                      │   ║
║  │ ⚙️ │  │    MAIN CONTENT                      │   ║
║  │    │  │                                      │   ║
║  │ 👤 │  ╰──────────────────────────────────────╯   ║
║  ╰────╯                                             ║
╚══════════════════════════════════════════════════════╝
```

- Sidebar island shrinks to 60px, showing only icons
- Hovering an icon shows a **tooltip pill** floating to the right
- Smooth width transition animation (240px → 60px)

---

### Island Visual Layout — Mobile (<768px)

On mobile, the sidebar disappears entirely and a **bottom navigation island** appears:

```
╔═══════════════════════════════╗
║  [deep navy background]       ║
║                               ║
║  ╭─────────────────────────╮  ║
║  │  🔍 Search...    🔔  👤 │  ║
║  ╰─────────────────────────╯  ║
║                               ║
║  ╭─────────────────────────╮  ║
║  │                         │  ║
║  │   MAIN CONTENT          │  ║
║  │   (full width)          │  ║
║  │                         │  ║
║  │                         │  ║
║  ╰─────────────────────────╯  ║
║                               ║
║  ╭─────────────────────────╮  ║
║  │  🏠   🔐   🔑   🛡️   ⚙️ │  ║
║  ╰─────────────────────────╯  ║
╚═══════════════════════════════╝
```

- **Bottom navigation island** floats above the bottom edge with `--island-gap` margin
- The island has a **pill shape** with equal rounded corners
- Active item has a **pill-shaped highlight** that slides between items
- The topbar island is **compact** on mobile (no breadcrumbs, smaller search)
- Content area takes **full width** minus the island gaps

---

### Island Interaction States

#### Hover State
When hovering over an island (or hovering the sidebar), the island subtly **brightens and lifts**:

```scss
.sidebar-island:hover,
.topbar-island:hover {
  background: rgba(26, 34, 53, 0.90);  // Slightly more opaque
  box-shadow: var(--island-shadow-hover);  // Enhanced glow shadow
  transform: translateY(-1px);  // Subtle lift
  transition: all var(--transition-base);
}
```

#### Active/Focus State
When an island contains an active route or focused element, it gets a **subtle accent glow border**:

```scss
.sidebar-island.has-active-route {
  border-color: rgba(99, 102, 241, 0.2);  // Indigo glow border
  box-shadow: var(--island-shadow),
              0 0 0 1px rgba(99, 102, 241, 0.1);  // Inner glow ring
}
```

#### Scroll Behavior
- **Topbar island:** Stays fixed at top — does NOT scroll with content
- **Sidebar island:** Stays fixed on left — does NOT scroll with content
- **Content island:** Scrolls internally (overflow-y: auto with custom scrollbar)
- **Bottom nav island:** Stays fixed at bottom — does NOT scroll

#### Collapse Animation (Sidebar)
```scss
// Sidebar collapse: 240px → 60px
.sidebar-island {
  width: 240px;
  transition: width 300ms cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &.collapsed {
    width: 60px;

    .nav-label { opacity: 0; width: 0; }
    .nav-icon  { margin: 0 auto; }
  }
}
```

---

### Island SCSS Implementation

#### `_island-system.scss` (new file)

```scss
// ============================================================
// ISLAND DESIGN SYSTEM
// Floating, detached, pill-shaped navigation elements
// ============================================================

// Island base mixin — apply to any floating navigation element
@mixin island-base($radius: var(--island-radius)) {
  position: fixed;
  border-radius: $radius;
  background: var(--island-bg);
  backdrop-filter: var(--island-blur);
  -webkit-backdrop-filter: var(--island-blur);
  border: var(--island-border);
  box-shadow: var(--island-shadow);
  transition:
    box-shadow var(--transition-base),
    background var(--transition-base),
    transform var(--transition-base);
  z-index: 100;

  &:hover {
    box-shadow: var(--island-shadow-hover);
  }
}

// Sidebar island
.sidebar-island {
  @include island-base;
  top: var(--island-gap-lg);
  left: var(--island-gap-lg);
  bottom: var(--island-gap-lg);
  width: 240px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  @media (max-width: 991px) {
    width: 60px;
  }

  @media (max-width: 767px) {
    display: none;  // Hidden on mobile
  }
}

// Topbar island
.topbar-island {
  @include island-base;
  top: var(--island-gap-lg);
  left: calc(240px + var(--island-gap-lg) * 2);
  right: var(--island-gap-lg);
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;

  @media (max-width: 991px) {
    left: calc(60px + var(--island-gap-lg) * 2);
  }

  @media (max-width: 767px) {
    left: var(--island-gap);
    right: var(--island-gap);
    top: var(--island-gap);
    height: 52px;
  }
}

// Main content island
.content-island {
  position: fixed;
  top: calc(60px + var(--island-gap-lg) * 2);
  left: calc(240px + var(--island-gap-lg) * 2);
  right: var(--island-gap-lg);
  bottom: var(--island-gap-lg);
  border-radius: var(--island-radius);
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  overflow-y: auto;
  overflow-x: hidden;

  // Custom scrollbar inside content island
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: var(--border-default);
    border-radius: 3px;
  }

  @media (max-width: 991px) {
    left: calc(60px + var(--island-gap-lg) * 2);
  }

  @media (max-width: 767px) {
    top: calc(52px + var(--island-gap) * 2);
    left: var(--island-gap);
    right: var(--island-gap);
    bottom: calc(64px + var(--island-gap) * 2);  // Space for bottom nav
  }
}

// Bottom navigation island (mobile only)
.bottom-nav-island {
  @include island-base;
  bottom: var(--island-gap);
  left: var(--island-gap);
  right: var(--island-gap);
  height: 64px;
  display: none;  // Hidden on desktop/tablet
  align-items: center;
  justify-content: space-around;
  padding: 0 8px;

  @media (max-width: 767px) {
    display: flex;
  }
}

// Active nav item pill indicator inside sidebar
.nav-item-pill {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 14px;  // Inner pill for nav items
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  &.active {
    background: rgba(99, 102, 241, 0.15);  // Indigo tint
    color: var(--accent-primary);
    font-weight: 600;

    // Glowing left accent bar
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 60%;
      background: var(--accent-primary);
      border-radius: 0 2px 2px 0;
      box-shadow: 0 0 8px var(--glow-primary);
    }
  }
}

// Bottom nav active pill indicator
.bottom-nav-pill {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);
  font-size: 10px;

  &.active {
    background: rgba(99, 102, 241, 0.15);
    color: var(--accent-primary);
  }

  // Sliding active indicator (animated via Angular)
  .active-indicator {
    position: absolute;
    bottom: -4px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--accent-primary);
    box-shadow: 0 0 6px var(--glow-primary);
  }
}
```

---

### Island Entrance Animations

When the app first loads, the islands **animate in** from their respective edges:

```scss
// Sidebar island: slides in from left
@keyframes islandEnterLeft {
  from {
    opacity: 0;
    transform: translateX(-20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

// Topbar island: slides down from top
@keyframes islandEnterTop {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

// Bottom nav island: slides up from bottom
@keyframes islandEnterBottom {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

// Content island: fades and scales in
@keyframes islandEnterContent {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

// Apply animations on app init
.sidebar-island    { animation: islandEnterLeft    400ms var(--spring) both; }
.topbar-island     { animation: islandEnterTop     400ms var(--spring) 100ms both; }
.content-island    { animation: islandEnterContent 400ms var(--spring) 200ms both; }
.bottom-nav-island { animation: islandEnterBottom  400ms var(--spring) 100ms both; }
```

---

### Island Responsive Summary Table

| Element | Desktop (≥992px) | Tablet (768–991px) | Mobile (<768px) |
|---|---|---|---|
| **Sidebar Island** | Fixed left, 240px wide, full height minus gaps | Fixed left, 60px wide (icon-only) | Hidden |
| **Topbar Island** | Fixed top, spans from sidebar to right edge | Fixed top, spans from icon sidebar to right | Fixed top, full width minus gaps |
| **Content Island** | Fills remaining space (right of sidebar, below topbar) | Fills remaining space | Full width, between topbar and bottom nav |
| **Bottom Nav Island** | Hidden | Hidden | Fixed bottom, full width minus gaps, 64px tall |
| **Gap from edges** | 20px all sides | 16px all sides | 12px all sides |
| **Border radius** | 24px | 24px | 18px |

---

## 🏗️ Project Architecture

```
password-manager-frontend/
├── src/
│   ├── app/
│   │   ├── core/                          # Singleton services, guards, interceptors
│   │   │   ├── services/
│   │   │   │   ├── api.service.ts         # Base HTTP wrapper
│   │   │   │   ├── auth.service.ts        # Auth + token management
│   │   │   │   ├── vault.service.ts       # Vault CRUD
│   │   │   │   ├── generator.service.ts   # Password generator
│   │   │   │   ├── security.service.ts    # Audit, alerts, breach
│   │   │   │   ├── notification.service.ts
│   │   │   │   ├── settings.service.ts
│   │   │   │   ├── session.service.ts
│   │   │   │   ├── backup.service.ts
│   │   │   │   ├── share.service.ts
│   │   │   │   ├── timeline.service.ts
│   │   │   │   ├── dashboard.service.ts
│   │   │   │   ├── expiry.service.ts
│   │   │   │   ├── emergency.service.ts
│   │   │   │   ├── files.service.ts
│   │   │   │   ├── ai.service.ts
│   │   │   │   ├── teams.service.ts
│   │   │   │   └── health.service.ts
│   │   │   ├── state/                     # Angular Signals state
│   │   │   │   ├── auth.state.ts
│   │   │   │   ├── vault.state.ts
│   │   │   │   ├── ui.state.ts
│   │   │   │   └── notification.state.ts
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   ├── guest.guard.ts
│   │   │   │   └── read-only.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts    # JWT attachment + refresh
│   │   │   │   └── error.interceptor.ts   # Global error handling
│   │   │   └── models/                    # TypeScript interfaces
│   │   │       ├── auth.models.ts
│   │   │       ├── vault.models.ts
│   │   │       ├── security.models.ts
│   │   │       └── ...
│   │   ├── shared/                        # Reusable components
│   │   │   ├── components/
│   │   │   │   ├── pill-button/
│   │   │   │   ├── pill-input/
│   │   │   │   ├── pill-badge/
│   │   │   │   ├── pill-card/
│   │   │   │   ├── strength-meter/
│   │   │   │   ├── copy-button/
│   │   │   │   ├── toast/
│   │   │   │   ├── modal/
│   │   │   │   ├── confirm-dialog/
│   │   │   │   ├── skeleton-loader/
│   │   │   │   ├── empty-state/
│   │   │   │   └── avatar/
│   │   │   └── pipes/
│   │   │       ├── time-ago.pipe.ts
│   │   │       ├── file-size.pipe.ts
│   │   │       └── mask-password.pipe.ts
│   │   ├── features/
│   │   │   ├── auth/                      # Login, Register, Recovery
│   │   │   ├── vault/                     # Main vault dashboard
│   │   │   ├── generator/                 # Password generator
│   │   │   ├── security/                  # Audit, alerts, breach
│   │   │   ├── settings/                  # Profile, 2FA, sessions
│   │   │   ├── backup/                    # Export/Import
│   │   │   ├── sharing/                   # Secure sharing
│   │   │   ├── teams/                     # Team vault
│   │   │   ├── files/                     # File storage
│   │   │   ├── ai-assistant/              # AI chat
│   │   │   ├── emergency/                 # Emergency access
│   │   │   └── dashboard/                 # Analytics dashboard
│   │   ├── layout/
│   │   │   ├── main-layout/               # Authenticated shell
│   │   │   ├── auth-layout/               # Auth pages shell
│   │   │   ├── sidebar/
│   │   │   └── topbar/
│   │   ├── app.component.ts
│   │   ├── app.config.ts
│   │   └── app.routes.ts
│   ├── assets/
│   │   ├── icons/                         # SVG icon set
│   │   └── images/
│   └── styles/
│       ├── _variables.scss                # Design tokens (colors, spacing, radius)
│       ├── _pill-system.scss              # Pill component base classes
│       ├── _island-system.scss            # 🏝️ Island navigation (sidebar/topbar/bottom-nav floating)
│       ├── _animations.scss               # All keyframes (incl. island entrance animations)
│       ├── _bootstrap-override.scss       # Bootstrap customization (pill radius overrides)
│       ├── _typography.scss
│       ├── _utilities.scss
│       └── styles.scss                    # Global entry
├── angular.json
├── package.json
└── tsconfig.json
```

---

## 📦 Dependencies

```json
{
  "dependencies": {
    "@angular/core": "^18.0.0",
    "@angular/common": "^18.0.0",
    "@angular/forms": "^18.0.0",
    "@angular/router": "^18.0.0",
    "@angular/animations": "^18.0.0",
    "@angular/cdk": "^18.0.0",
    "bootstrap": "^5.3.3",
    "@popperjs/core": "^2.11.8",
    "chart.js": "^4.4.0",
    "ng2-charts": "^6.0.0",
    "qrcode": "^1.5.3",
    "rxjs": "^7.8.0",
    "crypto-js": "^4.2.0"
  },
  "devDependencies": {
    "@angular/cli": "^18.0.0",
    "sass": "^1.70.0",
    "typescript": "^5.4.0"
  }
}
```

---

---

# 🚀 PHASE 1 — Foundation & Design System
**Duration:** ~3 days  
**Goal:** Project scaffold, design system, shared components, routing skeleton

---

## 1.1 Project Setup

### Commands
```bash
ng new password-manager-frontend --routing --style=scss --standalone
cd password-manager-frontend
npm install bootstrap @popperjs/core chart.js ng2-charts qrcode crypto-js
```

### `angular.json` — Bootstrap Integration
```json
"styles": [
  "node_modules/bootstrap/dist/css/bootstrap.min.css",
  "src/styles/styles.scss"
],
"scripts": [
  "node_modules/bootstrap/dist/js/bootstrap.bundle.min.js"
]
```

---

## 1.2 Global SCSS Design System

### `src/styles/_variables.scss`
All CSS custom properties (design tokens) defined here. Pill radius, color palette, spacing, shadows, transitions.

### `src/styles/_pill-system.scss`
Base pill component classes:
- `.pill-btn` — pill-shaped button base
- `.pill-input` — pill-shaped form input
- `.pill-card` — pill-shaped card container
- `.pill-badge` — small pill badge/tag
- `.pill-chip` — interactive chip/filter
- `.pill-panel` — large rounded panel

### `src/styles/_animations.scss`
All `@keyframes` definitions:
- `slideUpFade` — page/section entrance
- `slideInRight` — side panel entrance
- `fadeScale` — modal/dropdown entrance
- `shimmer` — skeleton loading
- `strengthFill` — password strength bar
- `pillFloat` — hero element floating
- `pillPulse` — active state glow pulse
- `bounceIn` — notification/toast entrance
- `spinnerRotate` — loading spinner

### `src/styles/_bootstrap-override.scss`
Override Bootstrap variables to match pill design:
```scss
$border-radius:       28px;
$border-radius-sm:    20px;
$border-radius-lg:    40px;
$border-radius-xl:    60px;
$border-radius-pill:  9999px;
$primary:             #6366f1;
$success:             #10b981;
$warning:             #f59e0b;
$danger:              #ef4444;
$info:                #3b82f6;
$body-bg:             #0a0f1e;
$body-color:          #f1f5f9;
$card-bg:             #111827;
$card-border-radius:  $border-radius-lg;
```

---

## 1.3 Shared Component Library

### `PillButtonComponent`
```typescript
// Inputs: variant, size, loading, disabled, icon
// Variants: primary | secondary | danger | ghost | outline
// Sizes: sm | md | lg
// Features: loading spinner, icon slot, ripple effect on click
```

### `PillInputComponent`
```typescript
// Inputs: type, placeholder, label, error, hint, icon
// Features: password toggle eye icon, copy button, strength indicator
// Animations: label float on focus, error shake animation
```

### `PillCardComponent`
```typescript
// Inputs: title, subtitle, icon, variant, hoverable, clickable
// Features: hover elevation, glow border on hover, slot projection
```

### `StrengthMeterComponent`
```typescript
// Inputs: score (0-100), label, showDetails
// Visual: 5-segment pill bar, color transitions (red→orange→yellow→green)
// Animation: smooth fill animation on score change
```

### `CopyButtonComponent`
```typescript
// Inputs: value, label
// Features: clipboard copy, success animation (checkmark), tooltip
```

### `ToastComponent` + `ToastService`
```typescript
// Types: success | error | warning | info
// Features: auto-dismiss, progress bar, stack management
// Position: bottom-right, pill-shaped toast cards
```

### `SkeletonLoaderComponent`
```typescript
// Inputs: type (card | list | table | text), count
// Features: shimmer animation, responsive sizing
```

### `ModalComponent`
```typescript
// Inputs: title, size, closable
// Features: backdrop blur, slide-up entrance, pill-shaped container
```

### `ConfirmDialogComponent`
```typescript
// Inputs: title, message, confirmText, cancelText, variant (danger|warning)
// Features: keyboard ESC close, focus trap
```

### `EmptyStateComponent`
```typescript
// Inputs: icon, title, message, actionLabel
// Features: animated illustration, CTA button
```

### `AvatarComponent`
```typescript
// Inputs: username, size, showStatus
// Features: initials fallback, gradient background from username hash
```

---

## 1.4 Core Models (TypeScript Interfaces)

### `auth.models.ts`
```typescript
interface LoginRequest { username: string; masterPassword: string; captchaToken?: string; }
interface AuthResponse { accessToken: string; refreshToken: string; username: string; expiresIn: number; requires2FA: boolean; }
interface RegisterRequest { email: string; username: string; masterPassword: string; passwordHint?: string; securityQuestions: SecurityQuestion[]; }
interface SecurityQuestion { questionText: string; answer: string; }
interface TwoFactorSetupResponse { secret: string; qrCodeUrl: string; qrCodeImage: string; }
```

### `vault.models.ts`
```typescript
interface VaultEntry { id: number; title: string; username: string; password?: string; websiteUrl?: string; notes?: string; categoryId?: number; categoryName?: string; folderId?: number; folderName?: string; isFavorite: boolean; isHighlySensitive: boolean; createdAt: string; updatedAt: string; strengthScore: number; strengthLabel: string; }
interface VaultEntryRequest { title: string; username?: string; password?: string; websiteUrl?: string; notes?: string; categoryId?: number; folderId?: number; isFavorite?: boolean; isHighlySensitive?: boolean; }
interface TrashEntry { id: number; title: string; websiteUrl?: string; categoryName?: string; folderName?: string; deletedAt: string; expiresAt: string; daysRemaining: number; }
```

### `security.models.ts`
```typescript
interface AuditLog { id: number; action: string; details: string; ipAddress: string; createdAt: string; }
interface SecurityAlert { id: number; alertType: string; title: string; message: string; severity: 'LOW'|'MEDIUM'|'HIGH'|'CRITICAL'; isRead: boolean; createdAt: string; }
interface BreachStatus { status: 'SAFE'|'AT_RISK'; compromisedCount: number; lastScanAt: string; }
```

---

## 1.5 App Routing Skeleton

### `app.routes.ts`
```typescript
const routes: Routes = [
  // Auth layout routes (no sidebar)
  { path: 'auth', component: AuthLayoutComponent, children: [
    { path: 'login', loadComponent: () => LoginComponent },
    { path: 'register', loadComponent: () => RegisterComponent },
    { path: 'verify-email', loadComponent: () => VerifyEmailComponent },
    { path: 'forgot-password', loadComponent: () => ForgotPasswordComponent },
    { path: 'reset-password', loadComponent: () => ResetPasswordComponent },
    { path: '2fa', loadComponent: () => TwoFactorLoginComponent },
  ]},
  // Main app layout (with sidebar)
  { path: '', component: MainLayoutComponent, canActivate: [AuthGuard], children: [
    { path: 'dashboard', loadComponent: () => DashboardComponent },
    { path: 'vault', loadComponent: () => VaultComponent },
    { path: 'generator', loadComponent: () => GeneratorComponent },
    { path: 'security', loadComponent: () => SecurityComponent },
    { path: 'settings', loadComponent: () => SettingsComponent },
    { path: 'backup', loadComponent: () => BackupComponent },
    { path: 'sharing', loadComponent: () => SharingComponent },
    { path: 'teams', loadComponent: () => TeamsComponent },
    { path: 'files', loadComponent: () => FilesComponent },
    { path: 'ai', loadComponent: () => AiAssistantComponent },
    { path: 'emergency', loadComponent: () => EmergencyComponent },
    { path: 'trash', loadComponent: () => TrashComponent },
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  ]},
  // Public routes
  { path: 'share/:token', loadComponent: () => SharedPasswordViewComponent },
  { path: 'emergency/vault/:token', loadComponent: () => EmergencyVaultViewComponent },
  { path: '**', redirectTo: 'auth/login' },
];
```

---

---

# 🔐 PHASE 2 — Authentication System
**Duration:** ~4 days  
**APIs Used:** `/api/auth/*`, `/api/2fa/*`  
**Goal:** Complete auth flow — login, register, 2FA, recovery, email verification

---

## 2.1 Auth Layout Shell

### `AuthLayoutComponent`
- Full-screen split layout: left panel (branding/illustration) + right panel (form)
- Left panel: animated pill-shaped logo, tagline, floating security icons
- Right panel: white/dark pill-shaped form card, centered
- Responsive: stacks vertically on mobile (form on top)
- Background: animated gradient mesh with subtle pill shapes floating

**Visual Design:**
```
┌─────────────────────────────────────────────────────────┐
│  ╔═══════════════════╗  ╔═══════════════════════════╗   │
│  ║   🔐 RevaultX     ║  ║                           ║   │
│  ║                   ║  ║   ╭─────────────────╮     ║   │
│  ║  "Your passwords, ║  ║   │   Welcome Back  │     ║   │
│  ║   perfectly       ║  ║   │                 │     ║   │
│  ║   secured."       ║  ║   │  [username    ] │     ║   │
│  ║                   ║  ║   │  [password    ] │     ║   │
│  ║  ○ ○ ○ ○ ○        ║  ║   │  [  Sign In  ] │     ║   │
│  ╚═══════════════════╝  ║   ╰─────────────────╯     ║   │
│                         ╚═══════════════════════════╝   │
└─────────────────────────────────────────────────────────┘
```

---

## 2.2 Login Page

### `LoginComponent`
**Route:** `/auth/login`  
**APIs:** `POST /api/auth/login`, `GET /api/auth/password-hint/{username}`

**Features:**
- Pill-shaped username + password inputs with floating labels
- "Show password" toggle with eye icon animation
- Password hint display (after 2 failed attempts, fetch hint)
- CAPTCHA widget (shown after 3 failed attempts)
- "Remember me" pill toggle switch
- Animated submit button with loading state
- Error shake animation on failed login
- Smooth transition to 2FA page if `requires2FA: true`
- Link to forgot password, register

**State Machine:**
```
IDLE → LOADING → SUCCESS (redirect to dashboard)
                → REQUIRES_2FA (redirect to /auth/2fa)
                → CAPTCHA_REQUIRED (show CAPTCHA widget)
                → LOCKED_OUT (show countdown timer)
                → ERROR (shake animation + error message)
```

**CAPTCHA Integration:**
- After 3 failed attempts, show Google reCAPTCHA v3 or Cloudflare Turnstile widget
- Pass `captchaToken` in login request body

---

## 2.3 Two-Factor Authentication Login

### `TwoFactorLoginComponent`
**Route:** `/auth/2fa`  
**APIs:** `POST /api/auth/verify-otp`, `POST /api/auth/resend-otp`

**Features:**
- 6-digit OTP input — individual pill-shaped digit boxes
- Auto-advance focus between boxes on input
- Paste support (auto-fills all 6 boxes)
- Resend OTP button with 60-second cooldown timer
- Animated success checkmark on correct code
- Error shake + clear on wrong code
- Back to login link

**Visual Design:**
```
╭──────────────────────────────────╮
│  🔐 Two-Factor Authentication    │
│                                  │
│  Enter the 6-digit code from     │
│  your authenticator app          │
│                                  │
│  ╭─╮ ╭─╮ ╭─╮  ╭─╮ ╭─╮ ╭─╮      │
│  │3│ │7│ │2│  │8│ │1│ │5│      │
│  ╰─╯ ╰─╯ ╰─╯  ╰─╯ ╰─╯ ╰─╯      │
│                                  │
│  [    Verify Code    ]           │
│  Resend code in 0:45             │
╰──────────────────────────────────╯
```

---

## 2.4 Registration — Multi-Step Wizard

### `RegisterComponent`
**Route:** `/auth/register`  
**APIs:** `POST /api/auth/register`, `POST /api/generator/strength`

**Step 1 — Account Details:**
- Email input (with real-time format validation)
- Username input (with availability hint)
- Animated step indicator (pill-shaped progress bar)

**Step 2 — Master Password:**
- Password input with real-time strength meter
- Strength meter: 5-segment pill bar (Very Weak → Very Strong)
- Live feedback list: "Add uppercase", "Add numbers", etc.
- Password hint input (optional)
- Confirm password with match indicator

**Step 3 — Security Questions:**
- 2 security question dropdowns (predefined list)
- Answer inputs
- Explanation of why these are needed

**Step 4 — Review & Submit:**
- Summary of entered details (no password shown)
- Terms acceptance pill checkbox
- Animated submit with loading state

**Step Navigation:**
- Pill-shaped step indicators at top
- Next/Back pill buttons
- Smooth slide animation between steps

---

## 2.5 Email Verification

### `VerifyEmailComponent`
**Route:** `/auth/verify-email`  
**APIs:** `POST /api/auth/verify-email`, `POST /api/auth/resend-verification-otp`

**Features:**
- 6-digit OTP input (same as 2FA component)
- Email address display (masked: `j***@example.com`)
- Resend button with cooldown
- Success animation → redirect to login

---

## 2.6 Account Recovery Flow

### `ForgotPasswordComponent`
**Route:** `/auth/forgot-password`  
**APIs:** `POST /api/auth/forgot-password`, `GET /api/auth/security-questions/{username}`

**Step 1:** Enter username/email  
**Step 2:** Answer security questions (fetched from API)  
**Step 3:** Set new master password (with strength meter)  
**Step 4:** Success confirmation

### `ResetPasswordComponent`
**Route:** `/auth/reset-password`  
**APIs:** `POST /api/auth/reset-password`, `POST /api/auth/verify-security-questions`

---

## 2.7 Auth State Service

### `AuthStateService` (Angular Signals)
```typescript
// Signals
currentUser = signal<UserProfile | null>(null);
accessToken = signal<string | null>(null);  // In-memory only
isAuthenticated = computed(() => !!this.accessToken());
is2FAEnabled = signal<boolean>(false);
isReadOnly = signal<boolean>(false);

// Methods
login(credentials): Observable<AuthResponse>
logout(): Observable<void>
refreshToken(): Observable<AuthResponse>
verifyOtp(username, code): Observable<AuthResponse>
```

### `AuthInterceptor`
```typescript
// Attaches Bearer token to all requests
// Intercepts 401 → attempts token refresh → retries request
// On refresh failure → logout + redirect to login
```

---

---

# 🗄️ PHASE 3 — Main Layout & Navigation (Island Design)
**Duration:** ~3 days
**Goal:** App shell with floating island sidebar, island topbar, island bottom nav, responsive navigation

> **Key Principle:** All navigation elements are **floating islands** — detached from screen edges, pill-shaped, with frosted glass surfaces. See the [Island Design System](#️-island-design-system--floating-navigation-elements) section above for full SCSS implementation details.

---

## 3.1 Main Layout Shell

### `MainLayoutComponent`
The main layout is a **layered island composition** — all navigation elements float above the deep navy background. Nothing touches the screen edges. The background (`#0a0f1e`) is always visible in the gaps between islands, creating depth and a premium floating feel.

**Desktop Layout (≥992px):**
```
╔═══════════════════════════════════════════════════════════════════╗
║  [#0a0f1e deep navy background — always visible in all gaps]     ║
║                                                                   ║
║  ╭──────────╮  ╭─────────────────────────────────────────────╮   ║
║  │ SIDEBAR  │  │              TOPBAR ISLAND                  │   ║
║  │ ISLAND   │  │  [≡] [🔍 Search...]  [🔔3] [👤 JD] [⚡]   │   ║
║  │ 240px    │  ╰─────────────────────────────────────────────╯   ║
║  │ fixed    │                                                     ║
║  │ left     │  ╭─────────────────────────────────────────────╮   ║
║  │          │  │                                             │   ║
║  │ 🏠 Home  │  │         CONTENT ISLAND                     │   ║
║  │ 🔐 Vault │  │         (router-outlet, scrollable)        │   ║
║  │ 🔑 Gen   │  │                                             │   ║
║  │ 🛡️ Sec   │  │                                             │   ║
║  │ ⚙️ Set   │  │                                             │   ║
║  │          │  │                                             │   ║
║  │ [avatar] │  ╰─────────────────────────────────────────────╯   ║
║  ╰──────────╯                                                     ║
╚═══════════════════════════════════════════════════════════════════╝
```

**Tablet Layout (768–991px):**
```
╔══════════════════════════════════════════════════════╗
║  [deep navy background]                              ║
║                                                      ║
║  ╭────╮  ╭──────────────────────────────────────╮   ║
║  │SIDE│  │  [🔍 Search...]  [🔔] [👤]           │   ║
║  │ 60 │  ╰──────────────────────────────────────╯   ║
║  │ px │                                             ║
║  │icon│  ╭──────────────────────────────────────╮   ║
║  │only│  │                                      │   ║
║  │    │  │    CONTENT ISLAND                    │   ║
║  │    │  │                                      │   ║
║  ╰────╯  ╰──────────────────────────────────────╯   ║
╚══════════════════════════════════════════════════════╝
```

**Mobile Layout (<768px):**
```
╔═══════════════════════════════╗
║  [deep navy background]       ║
║                               ║
║  ╭─────────────────────────╮  ║
║  │  [🔍 Search...]  [🔔][👤]│  ║  ← Topbar island (floating)
║  ╰─────────────────────────╯  ║
║                               ║  ← Gap (background visible)
║  ╭─────────────────────────╮  ║
║  │                         │  ║
║  │   CONTENT ISLAND        │  ║
║  │   (full width)          │  ║
║  │                         │  ║
║  ╰─────────────────────────╯  ║
║                               ║  ← Gap (background visible)
║  ╭─────────────────────────╮  ║
║  │  🏠   🔐   🔑   🛡️   ⚙️ │  ║  ← Bottom nav island (floating)
║  ╰─────────────────────────╯  ║
╚═══════════════════════════════╝
```

**Angular Template Structure:**
```html
<!-- MainLayoutComponent template -->
<div class="app-shell">
  <!-- Deep navy background always visible in gaps -->
  <div class="bg-layer"></div>

  <!-- Sidebar Island (desktop/tablet only) -->
  <nav class="sidebar-island" [class.collapsed]="sidebarCollapsed">
    <app-sidebar></app-sidebar>
  </nav>

  <!-- Topbar Island (all breakpoints) -->
  <header class="topbar-island">
    <app-topbar (toggleSidebar)="sidebarCollapsed = !sidebarCollapsed"></app-topbar>
  </header>

  <!-- Content Island (all breakpoints) -->
  <main class="content-island" [@contentAnimation]>
    <router-outlet></router-outlet>
  </main>

  <!-- Bottom Nav Island (mobile only, shown via CSS) -->
  <nav class="bottom-nav-island">
    <app-bottom-nav></app-bottom-nav>
  </nav>
</div>
```

**Responsive Behavior:**
- Desktop (≥992px): Sidebar island 240px wide, topbar island spans right of sidebar, content island fills remaining space
- Tablet (768–991px): Sidebar island collapses to 60px icon-only, topbar and content adjust left offset
- Mobile (<768px): Sidebar island hidden, topbar island full width, content island full width, bottom nav island appears

---

## 3.2 Sidebar Island

### `SidebarComponent`
**CSS Class:** `.sidebar-island`
**Position:** `position: fixed`, left side, full height minus `--island-gap-lg` (20px) on all sides
**Visual:** Frosted glass pill-shaped panel — `border-radius: 24px`, `backdrop-filter: blur(20px)`, semi-transparent dark background

**The sidebar is a floating island — it does NOT touch the left edge, top edge, or bottom edge of the screen. There is always a 20px gap of deep navy background visible around it.**

**Internal Structure:**
```
╭──────────────────────────╮
│  🔐 RevaultX    [≡]      │  ← Logo + collapse toggle (pill button)
│  ─────────────────────── │  ← Subtle divider
│  ╭──────────────────────╮│
│  │ 🏠 Dashboard         ││  ← Active: indigo tint bg + left glow bar
│  ╰──────────────────────╯│
│  ╭──────────────────────╮│
│  │ 🔐 Vault          ▾  ││  ← Expandable group (pill toggle)
│  │   All Passwords      ││
│  │   ⭐ Favorites        ││
│  │   🕐 Recently Used   ││
│  │   🗑️ Trash  [3]      ││  ← Pill badge count
│  ╰──────────────────────╯│
│  📁 Folders           ▾  │
│  🏷️ Categories        ▾  │
│  🔑 Generator            │
│  🛡️ Security          ▾  │
│  │   Audit Logs          │
│  │   Alerts  [🔴 2]      │  ← Unread alert badge
│  │   Login History       │
│  │   Breach Monitor      │
│  📊 Dashboard            │
│  🤖 AI Assistant         │
│  👥 Teams                │
│  📎 Files                │
│  🔗 Sharing              │
│  ⚙️ Settings             │
│  ─────────────────────── │
│  ╭──────────────────────╮│
│  │ 👤 John Doe          ││  ← User section (pill card at bottom)
│  │ john@example.com     ││
│  │ [Logout]             ││
│  ╰──────────────────────╯│
╰──────────────────────────╯
```

**Navigation Item Design (`.nav-item-pill`):**
Each nav item is a **mini pill** inside the sidebar island:
- `border-radius: 14px` — inner pill shape
- Inactive: transparent background, `var(--text-secondary)` color
- Hover: `var(--bg-hover)` background, `var(--text-primary)` color
- Active: `rgba(99, 102, 241, 0.15)` indigo tint + left accent bar with glow:
  ```scss
  &.active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 60%;
    background: var(--accent-primary);
    border-radius: 0 2px 2px 0;
    box-shadow: 0 0 8px var(--glow-primary);
  }
  ```

**Collapsed State (Tablet — 60px icon-only):**
- Labels hidden: `opacity: 0; width: 0; overflow: hidden`
- Icons centered: `margin: 0 auto`
- Hovering an icon shows a **floating tooltip pill** to the right of the sidebar island:
  ```scss
  .nav-tooltip {
    position: absolute;
    left: calc(100% + 12px);  // Floats outside the sidebar island
    background: var(--bg-elevated);
    border: var(--island-border);
    border-radius: 10px;
    padding: 6px 12px;
    white-space: nowrap;
    box-shadow: var(--island-shadow);
    animation: fadeScale 150ms ease both;
    z-index: 200;
  }
  ```

**Sidebar island entrance animation:**
```scss
.sidebar-island {
  animation: islandEnterLeft 400ms var(--spring) both;
}
```

---

## 3.3 Topbar Island

### `TopbarComponent`
**CSS Class:** `.topbar-island`
**Position:** `position: fixed`, top, spanning from sidebar's right edge + gap to viewport right minus gap
**Visual:** Frosted glass pill-shaped bar — `border-radius: 24px`, 60px tall, `backdrop-filter: blur(20px)`

**The topbar is a floating island — it does NOT touch the top edge, left edge, or right edge of the screen. There is always a 20px gap of deep navy background visible above and around it.**

**Internal Layout:**
```
╭──────────────────────────────────────────────────────────────────╮
│  [≡]  [🔍 Search vault...                    ]  [🔔 3]  [👤 JD] │
╰──────────────────────────────────────────────────────────────────╯
```

**Elements inside the topbar island:**

1. **Sidebar Toggle Button** (leftmost, pill icon button)
   - Toggles sidebar between 240px ↔ 60px (desktop/tablet)
   - Hidden on mobile (no sidebar on mobile)

2. **Global Search Bar** (center, pill-shaped, expands on focus)
   - `border-radius: 9999px` — perfect pill shape
   - Placeholder: "Search vault..."
   - On focus: width expands with smooth transition, subtle glow border
   - Dropdown results: a **floating island panel** below the topbar island
   - Debounced 300ms → `GET /api/vault/search`
   - Keyboard navigation (↑↓, Enter, Esc)

3. **Notification Bell** (right side, pill icon button)
   - Unread count badge: small pill badge (red/indigo)
   - Click → **floating notification island panel** drops below topbar
   - Panel: pill-shaped, frosted glass, shows recent notifications

4. **User Avatar** (rightmost, circular pill)
   - Initials with gradient background
   - Click → **floating user menu island** drops below topbar
   - Menu: pill-shaped panel with Profile, Settings, Read-Only toggle, Logout

5. **System Health Dot** (far right, 8px dot)
   - Green (UP), Yellow (DEGRADED), Red (DOWN)
   - Polls `GET /api/health` every 5 minutes
   - Tooltip on hover

**Topbar island entrance animation:**
```scss
.topbar-island {
  animation: islandEnterTop 400ms var(--spring) 100ms both;
}
```

---

## 3.4 Bottom Navigation Island (Mobile)

### `BottomNavComponent`
**CSS Class:** `.bottom-nav-island`
**Position:** `position: fixed`, bottom, full width minus `--island-gap` (12px) on left/right/bottom
**Visual:** Frosted glass pill-shaped bar — `border-radius: 18px`, 64px tall
**Visibility:** Only shown on mobile (<768px) via `display: none` → `display: flex`

**The bottom nav is a floating island — it does NOT touch the bottom edge, left edge, or right edge of the screen. There is always a 12px gap of deep navy background visible below and around it.**

**Internal Layout:**
```
╭──────────────────────────────────────────────────────╮
│                                                      │
│   🏠      🔐      🔑      🛡️      ⚙️               │
│  Home   Vault   Gen    Security  Settings            │
│                                                      │
╰──────────────────────────────────────────────────────╯
```

**Active State — Sliding Pill Indicator:**
The active item has a **pill-shaped highlight background** that **slides** between items using Angular animations:

```typescript
// Angular animation for sliding active background pill
export const bottomNavAnimation = trigger('slideIndicator', [
  state('home',     style({ transform: 'translateX(0%)' })),
  state('vault',    style({ transform: 'translateX(100%)' })),
  state('gen',      style({ transform: 'translateX(200%)' })),
  state('security', style({ transform: 'translateX(300%)' })),
  state('settings', style({ transform: 'translateX(400%)' })),
  transition('* <=> *', animate('250ms cubic-bezier(0.4, 0, 0.2, 1)')),
]);
```

```scss
.bottom-nav-island {
  position: relative;

  // Sliding active background pill (absolutely positioned)
  .active-bg-pill {
    position: absolute;
    width: 20%;       // 1/5 of nav width
    height: calc(100% - 12px);
    top: 6px;
    border-radius: 14px;
    background: rgba(99, 102, 241, 0.15);  // Indigo tint
    transition: transform 250ms cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: none;
    z-index: 0;
  }
}
```

**Each nav item (`.bottom-nav-pill`):**
- Icon (24px) + label (10px text) stacked vertically
- Active: indigo tint background (via sliding pill), indigo icon/text color
- Inactive: muted icon/text color
- Tap: scale down 0.95 → spring back (tactile feedback)

**Bottom nav island entrance animation:**
```scss
.bottom-nav-island {
  animation: islandEnterBottom 400ms var(--spring) 100ms both;
}
```

---

## 3.5 Content Island

### Content Area
**CSS Class:** `.content-island`
**Position:** `position: fixed`, fills the space between sidebar (left), topbar (top), and viewport edges (right/bottom) — all with `--island-gap-lg` margins
**Visual:** Rounded panel — `border-radius: 24px`, `background: var(--bg-surface)`, subtle border

**The content area is also an island — it does NOT touch any screen edge. It floats in the remaining space after the sidebar and topbar islands, with consistent gaps on all sides.**

```scss
.content-island {
  position: fixed;
  top: calc(60px + var(--island-gap-lg) * 2);      // Below topbar island + gap
  left: calc(240px + var(--island-gap-lg) * 2);    // Right of sidebar island + gap
  right: var(--island-gap-lg);
  bottom: var(--island-gap-lg);
  border-radius: var(--island-radius);             // 24px
  background: var(--bg-surface);                   // #111827
  border: 1px solid var(--border-subtle);
  overflow-y: auto;
  overflow-x: hidden;

  // Custom thin scrollbar
  scrollbar-width: thin;
  scrollbar-color: var(--border-default) transparent;

  // Mobile: full width, between topbar and bottom nav
  @media (max-width: 767px) {
    left: var(--island-gap);
    right: var(--island-gap);
    top: calc(52px + var(--island-gap) * 2);
    bottom: calc(64px + var(--island-gap) * 2);
  }
}
```

**Page transition inside content island:**
```typescript
export const contentAnimation = trigger('contentAnimation', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(12px)' }),
    animate('300ms cubic-bezier(0.4, 0, 0.2, 1)',
      style({ opacity: 1, transform: 'translateY(0)' }))
  ]),
]);
```

---

## 3.6 Island Spacing Reference

All islands maintain consistent gaps from the viewport edges and from each other:

```
Desktop (≥992px) — 20px gaps everywhere:
┌─────────────────────────────────────────────────────────────────┐
│ 20px gap                                                        │
│    ╭──────────╮ 20px ╭──────────────────────────────────────╮  │
│    │          │      │ TOPBAR ISLAND (60px tall)            │  │
│    │ SIDEBAR  │      ╰──────────────────────────────────────╯  │
│    │ ISLAND   │ 20px gap                                       │
│    │ (240px)  │      ╭──────────────────────────────────────╮  │
│    │          │      │                                      │  │
│    │          │      │ CONTENT ISLAND                       │  │
│    │          │      │                                      │  │
│    ╰──────────╯      ╰──────────────────────────────────────╯  │
│ 20px gap                                                   20px │
└─────────────────────────────────────────────────────────────────┘

Mobile (<768px) — 12px gaps everywhere:
┌─────────────────────────────────────┐
│ 12px gap                            │
│    ╭───────────────────────────╮    │
│    │ TOPBAR ISLAND (52px tall) │    │
│    ╰───────────────────────────╯    │
│ 12px gap                            │
│    ╭───────────────────────────╮    │
│    │                           │    │
│    │ CONTENT ISLAND            │    │
│    │                           │    │
│    ╰───────────────────────────╯    │
│ 12px gap                            │
│    ╭───────────────────────────╮    │
│    │ BOTTOM NAV ISLAND (64px)  │    │
│    ╰───────────────────────────╯    │
│ 12px gap                            │
└─────────────────────────────────────┘
```

**Island Responsive Summary:**

| Element | Desktop (≥992px) | Tablet (768–991px) | Mobile (<768px) |
|---|---|---|---|
| **Sidebar Island** | Fixed left, 240px, full height minus 20px gaps | Fixed left, 60px icon-only | Hidden |
| **Topbar Island** | Fixed top, right of sidebar, 60px tall | Fixed top, right of icon sidebar | Fixed top, full width minus 12px gaps, 52px tall |
| **Content Island** | Fills remaining space, 24px radius | Fills remaining space | Full width minus 12px gaps, between topbar and bottom nav |
| **Bottom Nav Island** | Hidden | Hidden | Fixed bottom, full width minus 12px gaps, 64px tall, 18px radius |
| **Gap from edges** | 20px all sides | 16px all sides | 12px all sides |
| **Border radius** | 24px | 24px | 18px (bottom nav), 24px (others) |

---

---

# 🔐 PHASE 4 — Vault System (Core Feature)
**Duration:** ~5 days  
**APIs Used:** `/api/vault/*`, `/api/categories/*`, `/api/folders/*`  
**Goal:** Full vault CRUD, search, filter, favorites, sensitive entries

---

## 4.1 Vault Dashboard

### `VaultComponent`
**Route:** `/vault`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│  All Passwords (25)    [+ Add Entry]  [⚙️ Sort/Filter]  │
├─────────────────────────────────────────────────────────┤
│  [🔍 Search...]  [Category ▼]  [Folder ▼]  [⭐ Favs]   │
├─────────────────────────────────────────────────────────┤
│  ╭──────────────────────────────────────────────────╮   │
│  │ 🌐 GitHub          johndoe    ●●●●●●  Strong  ⭐ │   │
│  ╰──────────────────────────────────────────────────╯   │
│  ╭──────────────────────────────────────────────────╮   │
│  │ 📧 Gmail           user@...   ●●●○○   Fair    🔒 │   │
│  ╰──────────────────────────────────────────────────╯   │
│  ...                                                    │
└─────────────────────────────────────────────────────────┘
```

**Features:**
- Virtual scrolling (CDK Virtual Scroll) for large vaults
- Each entry row: favicon/icon, title, username (masked), strength pill, favorite star, sensitive lock icon
- Click row → slide-out detail panel from right
- Bulk selection with checkboxes → bulk delete
- Sort by: Title, Created, Updated (pill toggle buttons)
- Filter pills: Category, Folder, Favorites, Sensitive
- Empty state with "Add your first password" CTA

---

## 4.2 Vault Entry Detail Panel

### `EntryDetailPanelComponent`
Slides in from the right (400px wide on desktop, full-screen on mobile)

**Tabs:**
1. **Details** — All entry fields
2. **History** — Password change timeline
3. **Activity** — Entry-specific timeline events

**Details Tab:**
```
╭──────────────────────────────────────╮
│  🌐 GitHub                    [✏️] [🗑️] │
│  ─────────────────────────────────── │
│  Username:  johndoe          [📋]    │
│  Password:  ●●●●●●●●         [👁️][📋] │
│  Website:   github.com       [🔗][📋] │
│  Category:  Work                     │
│  Folder:    Dev                      │
│  Notes:     Work account             │
│  ─────────────────────────────────── │
│  Strength:  ████████░░  85 Strong    │
│  Created:   Jan 1, 2026              │
│  Updated:   Jan 15, 2026             │
│  ─────────────────────────────────── │
│  [⭐ Favorite]  [🔒 Sensitive]       │
│  [🔗 Share]     [📤 Export]          │
╰──────────────────────────────────────╯
```

**Password Reveal:**
- Password shown as `●●●●●●●●` by default
- Click eye icon → calls `POST /api/vault/entries/{id}/view-password`
- 30-second auto-hide timer with countdown
- For sensitive entries → triggers `SensitiveUnlockOverlay`

---

## 4.3 Add/Edit Entry Modal

### `EntryFormModalComponent`
**APIs:** `POST /api/vault`, `PUT /api/vault/{id}`

**Form Fields:**
- Title (required) — pill input
- Username — pill input with copy button
- Password — pill input with:
  - Show/hide toggle
  - Strength meter (real-time, calls `POST /api/generator/strength`)
  - "Generate" button → opens inline generator
- Website URL — pill input with favicon preview
- Notes — pill textarea
- Category — pill select dropdown
- Folder — pill select dropdown (hierarchical)
- Favorite toggle — pill switch
- Highly Sensitive toggle — pill switch with warning

**Inline Password Generator:**
- Slides up from bottom of password field
- Length slider (8-128)
- Toggle pills: Uppercase, Lowercase, Numbers, Symbols
- Exclude Similar, Exclude Ambiguous toggles
- Generated password preview with copy button
- "Use This Password" button

---

## 4.4 Sensitive Entry Unlock Overlay

### `SensitiveUnlockOverlayComponent`
**APIs:** `POST /api/vault/{id}/sensitive-view`, `POST /api/auth/verify-master-password`

**Features:**
- Full-screen blur overlay
- Master password input
- Optional OTP input (if 2FA enabled)
- 30-second countdown timer after unlock
- Auto-lock after timeout

---

## 4.5 Favorites View

### `FavoritesComponent`
**Route:** `/vault/favorites`  
**API:** `GET /api/vault/favorites`

- Same layout as vault but filtered to favorites
- Quick-access grid layout (pill cards, 3 columns)
- Drag to reorder (future enhancement)

---

## 4.6 Categories Management

### `CategoriesComponent`
**APIs:** `GET /api/categories`, `POST /api/categories`, `PUT /api/categories/{id}`, `DELETE /api/categories/{id}`

**Features:**
- List of categories with entry counts
- Inline rename (click to edit)
- Color/icon picker for category
- Delete with confirmation (reassign entries option)
- Drag to reorder

---

## 4.7 Folders Management

### `FolderTreeComponent`
**APIs:** `GET /api/folders`, `POST /api/folders`, `PUT /api/folders/{id}`, `PUT /api/folders/{id}/move`, `DELETE /api/folders/{id}`

**Features:**
- Hierarchical tree view in sidebar
- Expand/collapse with animation
- Inline rename on double-click
- Right-click context menu: New Subfolder, Rename, Delete
- Drag-and-drop to move folders (calls `PUT /api/folders/{id}/move`)

---

## 4.8 Trash Bin

### `TrashComponent`
**Route:** `/trash`  
**APIs:** `GET /api/vault/trash`, `POST /api/vault/trash/{id}/restore`, `DELETE /api/vault/trash/{id}`, `DELETE /api/vault/trash/empty`

**Visual Design:**
- Red-tinted header with warning icon
- Each entry shows: title, deleted date, days remaining before permanent deletion
- Progress bar showing days remaining (red when < 7 days)
- Per-row: Restore button, Permanent Delete button
- "Restore All" and "Empty Trash" global actions
- Confirmation dialog for permanent delete / empty trash

---

## 4.9 Password History Timeline

### `PasswordHistoryComponent`
**API:** `GET /api/vault/entries/{id}/history`

**Features:**
- Vertical timeline inside Entry Detail Panel > History tab
- Each snapshot: date, masked password, "View" button
- View button reveals old password temporarily

---

---

# 🔑 PHASE 5 — Password Generator
**Duration:** ~2 days  
**APIs Used:** `/api/generator/*`  
**Goal:** Standalone generator page + inline widget

---

## 5.1 Generator Page

### `GeneratorComponent`
**Route:** `/generator`  
**APIs:** `POST /api/generator/generate`, `POST /api/generator/generate-multiple`, `POST /api/generator/strength`

**Layout:**
```
╭──────────────────────────────────────────────────────────╮
│  🔑 Password Generator                                   │
│                                                          │
│  ╭──────────────────────────────────────────────────╮   │
│  │  Xk9#mP2@nQ5!vR7$wZ3&                    [📋][🔄] │   │
│  ╰──────────────────────────────────────────────────╯   │
│                                                          │
│  Strength: ████████████░░░░  Strong (85)                │
│                                                          │
│  Length: ──────●──────────  16                          │
│                                                          │
│  [A-Z Uppercase ✓]  [a-z Lowercase ✓]                  │
│  [0-9 Numbers ✓]    [!@# Symbols ✓]                    │
│  [Exclude Similar]  [Exclude Ambiguous]                 │
│                                                          │
│  [Generate New]  [Generate 5 Options]  [Save to Vault]  │
│                                                          │
│  ── Recent Passwords ──────────────────────────────     │
│  Xk9#mP2@nQ5!vR7$  [📋]  Strong                        │
│  mP2@nQ5!vR7$wZ3&  [📋]  Strong                        │
╰──────────────────────────────────────────────────────────╯
```

**Features:**
- Real-time strength meter updates as options change
- Length slider with pill-shaped thumb
- Toggle pills for character types (active = filled pill, inactive = outline pill)
- "Generate 5 Options" → shows 5 passwords to choose from
- Recent passwords history (local session storage)
- "Save to Vault" → opens Add Entry modal pre-filled with generated password
- Entropy display and crack time estimate

---

## 5.2 Inline Generator Widget

### `InlineGeneratorWidgetComponent`
Used inside the Entry Form Modal:
- Compact version of the generator
- Slides up from password field
- "Use This Password" fills the form field

---

---

# 📊 PHASE 6 — Dashboard & Analytics
**Duration:** ~3 days  
**APIs Used:** `/api/dashboard/*`, `/api/users/dashboard`, `/api/users/activity-heatmap`  
**Goal:** Security score, password health charts, activity heatmap

---

## 6.1 Dashboard Overview

### `DashboardComponent`
**Route:** `/dashboard`

**Bento-Box Grid Layout:**
```
┌──────────────┬──────────────┬──────────────────────────┐
│  Security    │  Total       │  Password Health          │
│  Score       │  Passwords   │  Donut Chart              │
│  82 / 100    │  25          │  Strong/Fair/Weak         │
│  ████████░░  │              │                           │
├──────────────┴──────────────┤                           │
│  Quick Actions              │                           │
│  [+ Add] [🔑 Gen] [🔍 Scan] │                           │
├─────────────────────────────┴──────────────────────────┤
│  Activity Heatmap (GitHub-style)                        │
│  Mon ■ □ ■ ■ □ ■ □ ■ ■ □ ■ ■ □ ■ □ ■ ■ □ ■ □ ■ ■ □  │
│  Tue □ ■ □ □ ■ □ ■ □ □ ■ □ □ ■ □ ■ □ □ ■ □ ■ □ □ ■  │
│  ...                                                    │
├─────────────────────────────┬──────────────────────────┤
│  Security Trend (30 days)   │  Expiring Soon            │
│  Line chart                 │  3 passwords expire in    │
│                             │  < 7 days                 │
├─────────────────────────────┴──────────────────────────┤
│  Reused Passwords (2 groups)  │  Recent Activity        │
│  Gmail, Yahoo (same pass)     │  Timeline feed          │
└───────────────────────────────┴────────────────────────┘
```

---

## 6.2 Security Score Widget

### `SecurityScoreWidgetComponent`
**API:** `GET /api/dashboard/security-score`

- Circular progress ring (SVG) with animated fill
- Score label: Excellent/Good/Fair/Poor
- Breakdown: Strong/Fair/Weak/Reused/Old counts
- Recommendation text
- Color: green (80+), yellow (60-79), orange (40-59), red (<40)

---

## 6.3 Password Health Chart

### `PasswordHealthChartComponent`
**API:** `GET /api/dashboard/password-health`

- Donut chart (Chart.js) with 5 segments
- Legend with counts
- Click segment → filters vault to that category
- Category breakdown table below chart

---

## 6.4 Activity Heatmap

### `ActivityHeatmapComponent`
**API:** `GET /api/users/activity-heatmap`

- GitHub-style contribution grid (7 rows × 52 cols)
- Color intensity based on access count
- Tooltip on hover: date + count
- Most active day/hour display
- Pill-shaped legend

---

## 6.5 Security Trend Chart

### `SecurityTrendChartComponent`
**API:** `GET /api/dashboard/trends`

- Line chart (Chart.js) showing score over 30 days
- Trend direction indicator (↑ Improving / ↓ Declining)
- Score change badge (+7 this month)

---

## 6.6 Password Age Distribution

### `PasswordAgeChartComponent`
**API:** `GET /api/dashboard/password-age`

- Horizontal bar chart showing age distribution
- Color coding: green (fresh) → red (ancient)
- Average age display

---

---

# 🛡️ PHASE 7 — Security Center
**Duration:** ~4 days  
**APIs Used:** `/api/security/*`, `/api/expiry/*`  
**Goal:** Audit logs, alerts, login history, breach monitor, expiry tracker

---

## 7.1 Security Center Layout

### `SecurityComponent`
**Route:** `/security`

**Tabs:**
1. Overview (audit report)
2. Alerts
3. Login History
4. Audit Logs
5. Breach Monitor
6. Password Expiry

---

## 7.2 Security Overview

### `SecurityOverviewComponent`
**APIs:** `GET /api/security/audit-report`, `GET /api/security/breach-status`

- Security score card
- Weak passwords list with "Fix" buttons
- Reused passwords list
- Old passwords list
- Breach status banner (red if AT_RISK)
- "Run Full Scan" button

---

## 7.3 Security Alerts

### `SecurityAlertsComponent`
**APIs:** `GET /api/security/alerts`, `PUT /api/security/alerts/{id}/read`, `DELETE /api/security/alerts/{id}`

**Features:**
- Alert cards with severity color coding (pill badges)
- Severity: CRITICAL (red), HIGH (orange), MEDIUM (yellow), LOW (blue)
- Mark as read / delete per alert
- Filter by severity
- Unread count badge in sidebar

---

## 7.4 Login History

### `LoginHistoryComponent`
**API:** `GET /api/security/login-history`

**Features:**
- Table: IP, Device, Location, Status, Date
- Status pill: SUCCESS (green), FAILED (red)
- Filter by status
- Suspicious login detection (new location highlighted)

---

## 7.5 Audit Logs

### `AuditLogsComponent`
**API:** `GET /api/security/audit-logs`

**Features:**
- Dense data table with pagination
- Action type icons (LOGIN, VAULT_VIEW, ENTRY_DELETED, etc.)
- IP address display
- Date/time with relative time tooltip
- Filter by action type

---

## 7.6 Breach Monitor

### `BreachMonitorComponent`
**APIs:** `POST /api/security/breach-scan`, `GET /api/security/breach-status`, `GET /api/security/compromised-credentials`, `GET /api/security/breach-history`

**Features:**
- Breach status banner (SAFE = green, AT_RISK = red pulsing)
- "Scan Now" button with loading animation
- Compromised credentials list with pwned count
- "Mark as Resolved" per credential
- Scan history table
- Last scan timestamp

---

## 7.7 Password Expiry Tracker

### `ExpiryTrackerComponent`
**APIs:** `GET /api/expiry/status`, `GET /api/expiry/expiring-soon`, `GET /api/expiry/policy`, `PUT /api/expiry/policy`, `POST /api/expiry/snooze/{entryId}`

**Features:**
- Summary cards: Fresh / Aging / Expiring Soon / Expired
- Expiring soon list with days remaining progress bars
- Status pills: FRESH (green), AGING (yellow), EXPIRING_SOON (orange), EXPIRED (red)
- Snooze button per entry
- Policy settings: default expiry days, reminder days before
- Bulk update expired passwords

---

---

# ⚙️ PHASE 8 — Settings & Profile
**Duration:** ~3 days  
**APIs Used:** `/api/users/*`, `/api/settings`, `/api/sessions/*`, `/api/2fa/*`  
**Goal:** Profile, 2FA setup, sessions, security questions, preferences

---

## 8.1 Settings Layout

### `SettingsComponent`
**Route:** `/settings`

**Tabs:**
1. Profile
2. Security (2FA, Password, Security Questions)
3. Sessions
4. Preferences
5. Danger Zone

---

## 8.2 Profile Tab

### `ProfileTabComponent`
**APIs:** `GET /api/users/profile`, `PUT /api/users/profile`

**Features:**
- Avatar with initials (pill-shaped circle)
- Name, email, username display
- Edit name and phone number
- Account creation date
- Account age badge

---

## 8.3 Security Tab

### `SecurityTabComponent`

**Change Master Password:**
**API:** `PUT /api/users/change-password`
- Current password input
- New password with strength meter
- Confirm new password

**Two-Factor Authentication:**
**APIs:** `GET /api/2fa/status`, `POST /api/2fa/setup`, `POST /api/2fa/verify-setup`, `POST /api/2fa/disable`, `GET /api/2fa/backup-codes`, `POST /api/2fa/regenerate-codes`

- 2FA status toggle (enabled/disabled)
- Setup flow:
  1. Click "Enable 2FA"
  2. Show QR code (render `qrCodeImage` from API)
  3. Enter 6-digit verification code
  4. Show backup codes (pill-shaped code cards)
  5. Confirm saved backup codes
- Backup codes display with copy/download
- Regenerate backup codes button

**Security Questions:**
**APIs:** `GET /api/users/security-questions`, `PUT /api/users/security-questions`
- View current questions (answers hidden)
- Update questions (requires master password)

**Password Hint:**
**APIs:** `GET /api/auth/password-hint/{username}`, `PUT /api/auth/password-hint`
- View/update password hint

**Duress Password:**
**API:** `POST /api/auth/set-duress-password`
- Set duress password with explanation

---

## 8.4 Sessions Tab

### `SessionsTabComponent`
**APIs:** `GET /api/sessions`, `GET /api/sessions/current`, `DELETE /api/sessions/{id}`, `DELETE /api/sessions`

**Features:**
- List of active sessions with device info, IP, location, last active
- Current session highlighted with "This device" badge
- "Terminate" button per session
- "Log Out All Other Devices" button
- Session expiry countdown

---

## 8.5 Preferences Tab

### `PreferencesTabComponent`
**APIs:** `GET /api/settings`, `PUT /api/settings`

**Settings:**
- Theme: Light / Dark / System (pill toggle)
- Language selector
- Auto-logout timeout (slider: 5-60 minutes)
- Read-only mode toggle (with warning)
- Default generator settings

---

## 8.6 Danger Zone Tab

### `DangerZoneTabComponent`
**API:** `DELETE /api/users/account`, `POST /api/users/account/cancel-deletion`

**Features:**
- Red-themed section
- "Delete Account" button → multi-step confirmation modal
  1. Warning about 30-day grace period
  2. Type "DELETE" to confirm
  3. Enter master password
  4. Final confirmation
- Cancel deletion button (if deletion scheduled)

---

---

# 📦 PHASE 9 — Backup & Import/Export
**Duration:** ~2 days  
**APIs Used:** `/api/backup/*`  
**Goal:** Export vault, import vault, third-party migration

---

## 9.1 Backup Component

### `BackupComponent`
**Route:** `/backup`

**Tabs:**
1. Export
2. Import
3. Third-Party Import
4. Snapshots

---

## 9.2 Export Tab

### `ExportTabComponent`
**APIs:** `GET /api/backup/export`, `GET /api/backup/export/preview`

**Features:**
- Format selector: JSON / CSV (pill toggle)
- Optional encryption password
- Preview button (shows entry count)
- Export button → downloads file via Blob URL
- Master password re-verification required

---

## 9.3 Import Tab

### `ImportTabComponent`
**API:** `POST /api/backup/import`, `POST /api/backup/import/validate`

**Features:**
- File upload dropzone (pill-shaped, drag-and-drop)
- Format auto-detection
- Optional decryption password
- "Validate First" button → shows preview
- Import result: imported/skipped/failed counts
- Error list for failed entries

---

## 9.4 Third-Party Import

### `ThirdPartyImportComponent`
**APIs:** `POST /api/backup/import-external`, `GET /api/backup/import-external/formats`

**Features:**
- Source selector: Chrome / Firefox / LastPass / 1Password (pill cards)
- Instructions for each source (how to export CSV)
- File upload
- Import result summary

---

---

# 🔗 PHASE 10 — Secure Sharing
**Duration:** ~2 days  
**APIs Used:** `/api/shares/*`  
**Goal:** Create share links, manage shares, view shared passwords

---

## 10.1 Sharing Dashboard

### `SharingComponent`
**Route:** `/sharing`

**Tabs:**
1. My Shares (created by me)
2. Received Shares (shared with me)

---

## 10.2 Create Share

### `CreateShareModalComponent`
**API:** `POST /api/shares`

**Features:**
- Select vault entry (searchable dropdown)
- Recipient email input
- Permission selector: View Once / View Multiple / Temporary Access (pill radio)
- Max views input (for View Multiple)
- Expiry duration: 1h / 6h / 24h / 7d (pill selector)
- Generated share URL display with copy button
- Warning: encryption key in URL fragment

---

## 10.3 Shared Password View (Public)

### `SharedPasswordViewComponent`
**Route:** `/share/:token`  
**API:** `GET /api/shares/{token}`

**Features:**
- No authentication required
- Decrypt password using key from URL fragment
- Display: title, username, website, password (with copy buttons)
- Expiry countdown
- View count display
- Branded RevaultX header

---

## 10.4 Manage Shares

**API:** `DELETE /api/shares/{id}`

- List of active shares with status
- Revoke button per share
- Expiry date display
- View count display

---

---

# 👥 PHASE 11 — Teams & Family Vault
**Duration:** ~3 days  
**APIs Used:** `/api/teams/*`  
**Goal:** Create teams, manage members, share vault entries

---

## 11.1 Teams Dashboard

### `TeamsComponent`
**Route:** `/teams`

**Features:**
- List of teams user belongs to (pill cards)
- Role badge per team (OWNER/ADMIN/MEMBER/VIEWER)
- Create new team button
- Team entry count and member count

---

## 11.2 Team Detail

### `TeamDetailComponent`
**Route:** `/teams/:id`

**Tabs:**
1. Vault (shared entries)
2. Members
3. Activity

**Vault Tab:**
- List of shared vault entries
- Add entry to team (select from personal vault)
- Remove entry from team

**Members Tab:**
- Member list with roles
- Invite by email (role selector)
- Change role (OWNER only)
- Remove member

**Activity Tab:**
- Team activity log

---

---

# 📎 PHASE 12 — File Storage Vault
**Duration:** ~2 days  
**APIs Used:** `/api/files/*`  
**Goal:** Upload, browse, download encrypted files

---

## 12.1 Files Component

### `FilesComponent`
**Route:** `/files`

**Layout:**
- Folder tree (left panel)
- File grid/list (right panel)
- Storage usage bar (used/100MB)

**Features:**
- Drag-and-drop file upload
- File type icons (PDF, image, document, etc.)
- Download button per file
- Delete with confirmation
- Create folder
- Search files by name
- File size display

---

---

# 🤖 PHASE 13 — AI Assistant
**Duration:** ~2 days  
**APIs Used:** `/api/ai/*`  
**Goal:** Chat interface, security insights, AI password generation

---

## 13.1 AI Assistant Component

### `AiAssistantComponent`
**Route:** `/ai`

**Layout:**
```
╭──────────────────────────────────────────────────────────╮
│  🤖 AI Security Assistant                               │
│  ─────────────────────────────────────────────────────  │
│                                                          │
│  ╭──────────────────────────────────────────────────╮   │
│  │ 🤖 Based on your vault, you have 5 passwords...  │   │
│  ╰──────────────────────────────────────────────────╯   │
│                                                          │
│  ╭──────────────────────────────────────────────────╮   │
│  │ 👤 Which passwords should I update?              │   │
│  ╰──────────────────────────────────────────────────╯   │
│                                                          │
│  Quick Actions:                                          │
│  [Update old passwords] [Check breaches] [Generate]     │
│                                                          │
│  ─────────────────────────────────────────────────────  │
│  [Type a message...                          ] [Send]   │
╰──────────────────────────────────────────────────────────╯
```

**Features:**
- Chat bubble UI (user right, AI left)
- Quick action suggestion pills
- AI-generated password display with copy button
- Security insights panel
- Clear chat history button
- "AI Powered" vs "Rule-based" indicator badge

---

---

# 🆘 PHASE 14 — Emergency Access
**Duration:** ~2 days  
**APIs Used:** `/api/emergency/*`  
**Goal:** Emergency contacts, access requests, vault access

---

## 14.1 Emergency Access Component

### `EmergencyComponent`
**Route:** `/emergency`

**Tabs:**
1. My Contacts (people who can access my vault)
2. My Requests (vaults I've requested access to)
3. Incoming Requests (people requesting my vault)

**Features:**
- Add emergency contact (email, name, relationship, waiting period)
- Contact verification status badge
- Request access to another user's vault
- Approve/deny incoming requests
- Waiting period countdown timer
- Access token display (when approved)

---

## 14.2 Emergency Vault View (Public)

### `EmergencyVaultViewComponent`
**Route:** `/emergency/vault/:token`  
**API:** `GET /api/emergency/vault/{token}`

- Read-only vault metadata view
- No passwords shown
- Expiry countdown
- Owner username display

---

---

# 🔔 PHASE 15 — Notifications & Timeline
**Duration:** ~2 days  
**APIs Used:** `/api/notifications/*`, `/api/timeline/*`  
**Goal:** Notification center, activity timeline

---

## 15.1 Notification Center

### `NotificationCenterComponent`
Accessible from topbar bell icon

**APIs:** `GET /api/notifications`, `GET /api/notifications/unread-count`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/mark-all-read`, `DELETE /api/notifications/{id}`

**Features:**
- Dropdown panel from topbar
- Notification type icons (PASSWORD_EXPIRY, BREACH_DETECTED, etc.)
- Unread count badge
- Mark all read button
- Delete individual notifications
- Link to relevant section

---

## 15.2 Activity Timeline

### `TimelineComponent`
Accessible from Security section

**APIs:** `GET /api/timeline`, `GET /api/timeline/summary`, `GET /api/timeline/stats`

**Features:**
- Vertical timeline with category icons
- Filter by category: VAULT / AUTH / BREACH / SHARING / BACKUP / SECURITY
- Date range filter
- Summary statistics cards
- Daily activity bar chart

---

---

# 🌐 PHASE 16 — Advanced Features
**Duration:** ~3 days  
**Goal:** Autofill API integration, read-only mode, health check, duress mode UI

---

## 16.1 Read-Only Mode

### `ReadOnlyRibbonComponent`
- Persistent top banner when read-only mode is active
- "READ-ONLY MODE ACTIVE" with disable button
- All create/edit/delete buttons hidden via `*ngIf="!isReadOnly"`
- Synced to `SettingsStateService`

---

## 16.2 System Health Footer

### `SystemHealthComponent`
- Small dot in footer (green/yellow/red)
- Polls `GET /api/health` every 5 minutes
- Click → shows health details modal
- API latency display

---

## 16.3 Autofill Suggestions Page

### `AutofillComponent`
**APIs:** `POST /api/autofill/suggestions`, `GET /api/autofill/trusted-domains`

- URL input to test autofill suggestions
- Matching entries display with match type badges
- Trusted domains list

---

---

# 📱 PHASE 17 — Responsive Polish & Accessibility
**Duration:** ~2 days  
**Goal:** Full mobile optimization, accessibility, performance

---

## 17.1 Responsive Breakpoints

| Breakpoint | Width | Layout |
|---|---|---|
| xs | < 576px | Single column, bottom nav, full-screen modals |
| sm | 576-767px | Single column, bottom nav |
| md | 768-991px | Sidebar icon-only (60px), 2-column content |
| lg | 992-1199px | Full sidebar (240px), standard layout |
| xl | 1200-1399px | Full sidebar, wider content |
| xxl | ≥ 1400px | Full sidebar, max-width content container |

---

## 17.2 Accessibility (WCAG 2.1 AA)

- All interactive elements have ARIA labels
- Focus trap in modals
- Keyboard navigation for all features
- Screen reader announcements for dynamic content
- Color contrast ratios ≥ 4.5:1
- Skip navigation link
- Focus visible indicators (pill-shaped focus ring)

---

## 17.3 Performance Optimizations

- Lazy loading for all feature modules
- Virtual scrolling for vault list (CDK Virtual Scroll)
- Image lazy loading
- Debounced search (300ms)
- HTTP request caching with Angular Signals
- Skeleton loaders for all async content
- Service Worker for offline support (future)

---

---

# 🎨 PHASE 18 — Final Polish & Animations
**Duration:** ~2 days  
**Goal:** Micro-interactions, page transitions, loading states

---

## 18.1 Page Transitions

```typescript
// Route animation trigger
export const routeAnimations = trigger('routeAnimations', [
  transition('* <=> *', [
    style({ opacity: 0, transform: 'translateY(20px)' }),
    animate('300ms cubic-bezier(0.4, 0, 0.2, 1)',
      style({ opacity: 1, transform: 'translateY(0)' }))
  ])
]);
```

---

## 18.2 Micro-Interactions Checklist

- [ ] Button press: scale down 0.97 on click
- [ ] Card hover: translate up 4px + shadow increase
- [ ] Input focus: border glow animation
- [ ] Toggle switch: smooth pill slide
- [ ] Strength meter: animated fill on change
- [ ] Copy button: checkmark success animation
- [ ] Toast: bounce-in entrance, slide-out exit
- [ ] Modal: fade-scale entrance, fade-scale exit
- [ ] Sidebar item: pill indicator slide animation
- [ ] Notification badge: pulse animation when new
- [ ] Delete: shake animation before confirm
- [ ] Success: confetti or checkmark animation
- [ ] Loading: shimmer skeleton screens
- [ ] Empty state: subtle floating animation

---

## 18.3 Dark/Light Theme Toggle

```typescript
// ThemeService
setTheme(theme: 'dark' | 'light' | 'system'): void {
  document.documentElement.setAttribute('data-theme', theme);
  // Smooth transition: add transition class, change theme, remove after 300ms
}
```

CSS variables switch between dark/light values based on `[data-theme]` attribute.

---

---

# 📋 Development Checklist Summary

## Phase 1 — Foundation (3 days)
- [ ] Angular project setup with Bootstrap 5
- [ ] SCSS design system (variables, pill system, animations)
- [ ] Shared component library (PillButton, PillInput, PillCard, etc.)
- [ ] TypeScript models for all API responses
- [ ] App routing skeleton
- [ ] Core services skeleton
- [ ] Auth interceptor

## Phase 2 — Authentication (4 days)
- [ ] Auth layout shell
- [ ] Login page with CAPTCHA support
- [ ] 2FA login (6-digit OTP input)
- [ ] Registration wizard (4 steps)
- [ ] Email verification
- [ ] Forgot password / reset password flow
- [ ] Auth state service with Angular Signals

## Phase 3 — Layout & Navigation (3 days)
- [ ] Main layout shell (sidebar + topbar + content)
- [ ] Sidebar with folder tree and navigation
- [ ] Topbar with global search and notifications
- [ ] Bottom navigation (mobile)
- [ ] Responsive behavior at all breakpoints

## Phase 4 — Vault System (5 days)
- [ ] Vault dashboard with virtual scrolling
- [ ] Entry detail slide panel
- [ ] Add/Edit entry modal with inline generator
- [ ] Sensitive entry unlock overlay
- [ ] Favorites view
- [ ] Categories management
- [ ] Folder tree management
- [ ] Trash bin
- [ ] Password history timeline

## Phase 5 — Password Generator (2 days)
- [ ] Standalone generator page
- [ ] Inline generator widget
- [ ] Real-time strength meter
- [ ] Generate multiple options

## Phase 6 — Dashboard & Analytics (3 days)
- [ ] Security score widget
- [ ] Password health donut chart
- [ ] Activity heatmap
- [ ] Security trend line chart
- [ ] Password age distribution chart
- [ ] Quick actions panel

## Phase 7 — Security Center (4 days)
- [ ] Security overview with audit report
- [ ] Security alerts with severity filtering
- [ ] Login history table
- [ ] Audit logs table
- [ ] Breach monitor with scan trigger
- [ ] Password expiry tracker

## Phase 8 — Settings & Profile (3 days)
- [ ] Profile tab
- [ ] 2FA setup flow with QR code
- [ ] Sessions management
- [ ] Preferences (theme, auto-logout)
- [ ] Danger zone (account deletion)

## Phase 9 — Backup (2 days)
- [ ] Export (JSON/CSV)
- [ ] Import with validation
- [ ] Third-party import wizard
- [ ] Snapshot management

## Phase 10 — Sharing (2 days)
- [ ] Create share link modal
- [ ] Manage shares dashboard
- [ ] Public shared password view

## Phase 11 — Teams (3 days)
- [ ] Teams list
- [ ] Team detail (vault, members, activity)
- [ ] Invite members
- [ ] Role management

## Phase 12 — Files (2 days)
- [ ] File browser with folder tree
- [ ] Upload with drag-and-drop
- [ ] Download and delete

## Phase 13 — AI Assistant (2 days)
- [ ] Chat interface
- [ ] Security insights panel
- [ ] Quick action suggestions

## Phase 14 — Emergency Access (2 days)
- [ ] Emergency contacts management
- [ ] Access request flow
- [ ] Public emergency vault view

## Phase 15 — Notifications & Timeline (2 days)
- [ ] Notification center dropdown
- [ ] Activity timeline page

## Phase 16 — Advanced Features (3 days)
- [ ] Read-only mode ribbon
- [ ] System health footer
- [ ] Autofill suggestions page

## Phase 17 — Responsive Polish (2 days)
- [ ] Mobile optimization
- [ ] Accessibility audit
- [ ] Performance optimization

## Phase 18 — Final Polish (2 days)
- [ ] Page transition animations
- [ ] Micro-interaction audit
- [ ] Dark/light theme toggle
- [ ] Final QA pass

---

**Total Estimated Duration: ~48 development days**

---

## 🔑 Key Technical Decisions

| Decision | Choice | Reason |
|---|---|---|
| Framework | Angular 18 Standalone | Modern, no NgModules overhead |
| State Management | Angular Signals | Lightweight, built-in, reactive |
| CSS Framework | Bootstrap 5.3 + Custom SCSS | Grid system + pill overrides |
| Charts | Chart.js + ng2-charts | Lightweight, well-supported |
| Animations | Angular Animations + CSS | Native, performant |
| Token Storage | Access: memory, Refresh: localStorage | Security best practice |
| HTTP | Angular HttpClient + Interceptors | Built-in, interceptor support |
| Forms | Angular Reactive Forms | Type-safe, validation |
| Routing | Angular Router (lazy loading) | Built-in, code splitting |
| Icons | Bootstrap Icons + Custom SVGs | Consistent, lightweight |

---

## 🌐 API Integration Summary

| Feature | Endpoints Used |
|---|---|
| Auth | 15 endpoints (`/api/auth/*`) |
| 2FA | 6 endpoints (`/api/2fa/*`) |
| Vault | 20+ endpoints (`/api/vault/*`) |
| Categories | 5 endpoints (`/api/categories/*`) |
| Folders | 6 endpoints (`/api/folders/*`) |
| Generator | 5 endpoints (`/api/generator/*`) |
| Profile/Settings | 10 endpoints (`/api/users/*`, `/api/settings`) |
| Sessions | 5 endpoints (`/api/sessions/*`) |
| Security | 15 endpoints (`/api/security/*`) |
| Backup | 8 endpoints (`/api/backup/*`) |
| Notifications | 5 endpoints (`/api/notifications/*`) |
| Dashboard | 5 endpoints (`/api/dashboard/*`) |
| Sharing | 5 endpoints (`/api/shares/*`) |
| Timeline | 4 endpoints (`/api/timeline/*`) |
| Health | 3 endpoints (`/api/health/*`) |
| Autofill | 3 endpoints (`/api/autofill/*`) |
| Expiry | 5 endpoints (`/api/expiry/*`) |
| Emergency | 9 endpoints (`/api/emergency/*`) |
| Files | 7 endpoints (`/api/files/*`) |
| AI | 5 endpoints (`/api/ai/*`) |
| Teams | 11 endpoints (`/api/teams/*`) |
| **Total** | **~152 endpoints** | |
