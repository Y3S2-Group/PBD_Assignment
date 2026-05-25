# Vault - Personal Finance Management System

**SE3092 | Platform Based Development | Assignment 01**
**SLIIT Faculty of Computing | 2026 Semester 01**

A personal finance management Android application built for users with irregular multi-source income, designed to provide real financial clarity through expense tracking, budget planning, and goal-oriented savings.

---

## Team & Feature Allocation

| Member | Feature |
|--------|---------|
| Member 1 | Income Tracking & Multi-Source Management |
| Member 2 | Expense Tracking & Categorization |
| Member 3 | Budget Planning & Savings Goal Tracker |
| Member 4 | Dashboard Analytics & Financial Insights |

---

## Features

### Income Tracking
- Log income from multiple sources: Salary, Freelance, AdSense, Crypto, and Custom
- Multi-currency support with live exchange rate conversion (all amounts normalized to LKR)
- Recurring income templates with reminders
- Freelance invoice status tracking with project references
- Period filters: Weekly / Monthly / Yearly views with month-over-month comparison

### Expense Tracking
- Categorized expense logging with committed vs. discretionary spend classification
- Payment method tracking
- Real-time category totals and filtering

### Budget Planning
- Per-category monthly budget allocation
- Actual vs. budgeted spending comparison
- Savings deposits toward goals with streak tracking

### Savings Goal Tracker
- Goal creation with target amount, currency, and deadline
- Required monthly savings calculation
- On-track / Ahead / Behind status based on elapsed time vs. savings progress
- Visual goal progress on dashboard

### Dashboard & Analytics
- Financial health score (0–100) based on savings rate, expense control, and goal progress
- 6-month income and expense trend charts
- Category spending breakdown
- Goal progress widget

### Smart Notifications
- Daily financial digest alerts (overspending, goal milestones, health score)
- Recurring income reminders via AlarmManager

### Authentication & Profile
- Firebase email/password authentication
- Google Sign-In
- Biometric (fingerprint) unlock
- Dark mode, avatar customization

---

## Firebase Configuration

1. Create a project at [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app with package name `com.example.financeapp`.
3. Download `google-services.json` and place it in the `app/` directory.
4. Enable **Authentication** → Sign-in methods: **Email/Password** and **Google**.
5. Enable **Cloud Firestore** → Start in production mode.
6. Apply the Firestore Security Rules from `firestore.rules` in the project root.
7. The app uses **Room** for local caching; no additional local database setup is required.

---

## Build Instructions

**Prerequisites:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Minimum SDK: API 26 (Android 8.0) | Target SDK: 34+

**Steps:**

```bash
# Clone the repository
git clone <repo-url>
cd PBD_Assignment
```

1. Open the project in Android Studio.
2. Place `google-services.json` in `app/`.
3. Sync Gradle: **File → Sync Project with Gradle Files**.
4. Run on a physical device or emulator running Android 8.0+.

---

## Architecture

The application follows **MVVM (Model-View-ViewModel)** with a Clean Architecture layering:

```
UI Layer        →  Jetpack Compose Screens
ViewModel Layer →  StateFlow-based state, viewModelScope coroutines
Repository Layer→  Interface abstractions (domain/repository/)
Data Layer      →  Room (local cache) + Firebase Firestore (cloud sync)
```

**Key technologies:**
- **Language:** Kotlin (100%)
- **UI:** Jetpack Compose + Material Design 3
- **Navigation:** Navigation Compose (single-activity)
- **State:** StateFlow / collectAsState
- **Async:** Kotlin Coroutines (viewModelScope)
- **DI:** Hilt
- **Local DB:** Room (6 tables: Income, Expense, Goal, BudgetCategory, SavingsDeposit, RecurringIncome)
- **Remote DB:** Firebase Firestore (per-user UID-scoped collections)
- **Auth:** Firebase Authentication
- **Currency:** Live exchange rates via Retrofit, cached in DataStore

**Data sync:** On sign-in, Room is repopulated from Firestore. On sign-out, local Room data is cleared to prevent cross-account leakage on shared devices.
