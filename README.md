# 📚 Campus Pages

<div align="center">

### Discover • Learn • Share

A modern academic resource platform built for students to discover, organize, and share educational content across subjects, semesters, and branches.

Built with **Kotlin**, **Jetpack Compose**, **Firebase**, **Algolia Search**, and **Material 3**.

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-orange)
![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow)
![Algolia](https://img.shields.io/badge/Search-Algolia-5468FF)
![Status](https://img.shields.io/badge/Status-Active%20Development-success)

</div>

---

# ✨ Overview

Campus Pages is a student-centric academic platform that enables users to upload, discover, organize, and share educational resources from a centralized repository.

Whether you're searching for lecture notes, assignments, previous year question papers, cheat sheets, or curated YouTube playlists, Campus Pages provides a fast and organized experience designed specifically for college students.

---

# 🚀 Features

## 🔍 Smart Discovery

| Feature                   | Description                                         |
| ------------------------- | --------------------------------------------------- |
| Global Search             | Lightning-fast Algolia powered search               |
| Recent Searches           | Instantly revisit previous searches                 |
| Smart Search Cache        | Session-based in-memory cache for reduced API usage |
| Explore Feed              | Browse newly uploaded resources                     |
| Trending Resources        | Popular study materials ranked using engagement     |
| Subject Hubs              | Organized content by academic subjects              |
| Branch & Semester Support | Resources categorized by curriculum                 |
| Continue Reading          | Resume previously viewed resources                  |
| Recently Viewed           | Quick access to recent activity                     |

---

## 📚 Study Resources

Campus Pages currently supports multiple resource types:

* 📄 Notes
* 📝 Assignments
* 📑 Previous Year Question Papers (PYQs)
* ⚡ Cheat Sheets
* 🎥 Educational Videos
* 📺 YouTube Playlists

Each resource includes:

* Rich metadata
* Preview thumbnails
* Subject categorization
* Branch & semester mapping
* Engagement tracking
* Search indexing

---

## 📤 Upload & Share

| Feature                        | Description                                |
| ------------------------------ | ------------------------------------------ |
| PDF Uploads                    | Upload lecture notes and documents         |
| Image Uploads                  | Share handwritten notes and diagrams       |
| Multi-File Uploads             | Support for multiple attachments           |
| YouTube Resources              | Add educational videos and playlists       |
| Automatic Thumbnail Generation | Preview generation for supported resources |
| Metadata System                | Structured academic categorization         |

---

## 🔎 Search Experience

Powered by **Algolia Search**

Features include:

* Full-text search
* Subject-aware search
* Resource type filtering
* Thumbnail-rich search results
* Intelligent metadata chips
* Session-based search caching
* Automatic search index synchronization

---

## 👤 Personalization

* Personalized recommendations
* Continue Reading
* Recently Viewed
* Bookmark synchronization
* User profiles
* Upload history

---

## 📊 Engagement System

* Views
* Downloads
* Bookmarks
* Upvotes
* Trending Score
* Contributor Profiles
* Activity Tracking

---

## ⚡ Performance

* Real-time Firestore synchronization
* Algolia indexed search
* Lazy loading & pagination
* Thumbnail generation pipeline
* Optimized image loading
* Session-based search cache
* Offline-friendly architecture

---

# 🛠 Tech Stack

## Android

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose
* Coroutines
* Flow
* Coil

## Architecture

* MVVM
* Repository Pattern
* StateFlow
* Reactive UI
* Provider-agnostic Search Architecture

## Backend

* Firebase Authentication
* Cloud Firestore
* Firebase Storage
* Firebase Cloud Functions
* Firebase Cloud Messaging

## Search

* Algolia Search
* Search Index Synchronization
* Search Metadata Pipeline

---

# 🏗 Project Structure

```text
app/
├── data/
├── models/
├── navigation/
├── ui/
│   ├── components/
│   ├── screens/
│   ├── theme/
│   └── widgets/
├── viewmodel/
├── utils/
└── MainActivity.kt
```

---

# 📸 Screenshots

> Add screenshots of the Home, Explore, Search, Resource Details, Upload, and Profile screens here.

| Home       | Explore    | Search     |
| ---------- | ---------- | ---------- |
| Screenshot | Screenshot | Screenshot |

| Resource Details | Profile    | Upload     |
| ---------------- | ---------- | ---------- |
| Screenshot       | Screenshot | Screenshot |

---

# 🚀 Getting Started

## Prerequisites

* Android Studio (Latest Stable)
* JDK 17+
* Git

## Clone

```bash
git clone https://github.com/<owner>/<repository>.git
```

## Build

```bash
./gradlew assembleDebug
```

Or simply open the project in Android Studio and run the application.

---

# 🔥 Firebase Configuration

Campus Pages uses:

* Firebase Authentication
* Cloud Firestore
* Firebase Storage
* Firebase Cloud Functions
* Firebase Cloud Messaging

Place your Firebase configuration file in:

```text
app/google-services.json
```

For Algolia Search, configure the required credentials inside:

```text
local.properties
```

```properties
ALGOLIA_APP_ID=YOUR_APP_ID
ALGOLIA_SEARCH_KEY=YOUR_SEARCH_API_KEY
```

---

# 📈 Development Status

🟢 **Actively Developed**

Recent additions include:

* ✅ Algolia Search Integration
* ✅ Smart Search Cache
* ✅ Rich Search Cards
* ✅ Assignment & PYQ Metadata
* ✅ YouTube Playlist Support
* ✅ Automatic Thumbnail Synchronization
* ✅ Search Index Synchronization
* ✅ Performance Optimizations

More features are actively being developed.

---

# ⚠ Repository Policy

This repository is publicly visible for portfolio, educational, and showcase purposes.

### External Contributions

Currently:

* Pull Requests are not being accepted.
* External feature contributions are not being reviewed.
* Redistribution or commercial usage is prohibited without written permission.

All development is maintained by the project owners.

---

# 📄 License

No open-source license has been applied to this repository.

Viewing the source code **does not grant permission** to:

* Redistribute
* Modify
* Repackage
* Commercially use

the project without explicit written permission from the maintainers.

---

<div align="center">

Made with ❤️ for students.

</div>
