# NotesSharing

A modern student-focused platform for sharing academic notes, PDFs, images, and educational resources.

Built with:
- Kotlin
- Jetpack Compose
- Material 3
- MVVM Architecture
- Gradle Kotlin DSL

---

## Getting Started

### Prerequisites

- Android Studio (latest stable version recommended)
- JDK 17+
- Git

---

## Clone Repository

Open Android Studio:

```text
Get from Version Control
```

Paste repository URL:

```bash
https://github.com/<owner>/<repository>.git
```

Choose a folder and click **Clone**.

Wait for:

- Gradle Sync
- Dependency Downloads
- Project Indexing

to finish completely.

---

## Verify Setup

Run the application once before making any changes.

Confirm:

- Project builds successfully
- Application launches correctly

---

## Branch Workflow

⚠️ Do NOT work directly on `master`.

Every contributor should use their own branch.

### Create Your Branch

Example:

```bash
git checkout -b YourName
```

Push branch to GitHub:

```bash
git push -u origin YourName
```

Examples:

```bash
git checkout -b Pratyush
git push -u origin Pratyush
```

```bash
git checkout -b Rahul
git push -u origin Rahul
```

---

## Daily Development Workflow

Switch to your branch:

```bash
git checkout YourName
```

Get latest updates:

```bash
git pull
```

Make changes.

Commit changes:

```bash
git add .
git commit -m "Describe your changes"
```

Push changes:

```bash
git push
```

---

## Pull Requests

When a feature is complete:

1. Push your branch
2. Open GitHub
3. Create a Pull Request
4. Select:

```text
Base Branch: master
Compare Branch: YourName
```

5. Review changes
6. Merge into master

---

## After a Merge

Update your local repository:

```bash
git checkout master
git pull
```

Then return to your branch:

```bash
git checkout YourName
```

Bring latest master changes into your branch:

```bash
git merge master
```

---

## Project Structure

```text
app/
├── model/
├── repository/
├── state/
├── ui/
│   ├── components/
│   ├── screens/
│   └── theme/
├── viewmodel/
└── MainActivity.kt
```

---

## Current Features

- Explore feed
- Trending notes
- YouTube recommendations
- Study collections
- Subject hubs
- Contributor profiles
- Upload screen
  - PDFs
  - Images
  - YouTube links
- Local persistence
- Dark theme support
- Material 3 UI

---

## Development Status

🚧 Active Development

Features and architecture may change while development is in progress.
