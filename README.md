<div align="center">
  <h1>AidBridge</h1>
  <sub>March 06, 2026</sub>
  <p>Disaster Relief Coordinator - DRC</p>
</div>

> A real-time, AI-assisted mobile application system designed to coordinate emergency disaster relief, bridging the gap between Victims, Volunteers, and Sponsors through Smart Hubs.

## 📖 Table of Contents

- [About the Project](#-about-the-project)
- [Key Features](#-key-features)
- [System Architecture & Tech Stack](#-system-architecture--tech-stack)
- [Core Algorithms](#-core-algorithms)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)

## 🎯 About the Project

During natural disasters, the disparity between available resources and urgent needs often leads to chaos. **AidBridge** solves this by replacing scattered social media posts with a centralized, real-time platform.

It leverages **Geospatial routing (PostGIS)** to guide volunteers safely and uses **Algorithms** to prioritize life-threatening SOS requests over standard supply needs, ensuring help reaches those who need it most, exactly when they need it.

## ✨ Key Features

### 🆘 For Victims (Anonymous & Registered)

- **Quick SOS:** One-tap emergency alert sending GPS location and situation details (no login required).
- **Supply Requests:** Request specific categories of aid (Medicine, Clothes, Food, Water).
- **Live Tracking:** Real-time map tracking of the assigned volunteer's route.

### 🏃‍♂️ For Volunteers

- **Smart Dispatch:** Receive tasks based on proximity and priority score.
- **Optimized Routing:** Polyline navigation to the nearest Hub and then to the Victim.
- **Offline Mode:** Pre-download critical area maps and cache rescue histories.

### 📦 For Sponsors & Hub Staff (Inventory)

- **Smart Hub Selection:** Suggests the nearest Hub lacking the specific items being donated.
- **QR Code Logistics:** Generate QR codes for donations. Staff scan to perform **Atomic Inventory Updates**.
- **Real-time Metrics:** Impact dashboards showing total lives saved and items distributed.

## 🛠 System Architecture & Tech Stack

This project is built as a **Monorepo**, cleanly separating the Android client and the Spring Boot backend while maintaining unified documentation.

### Frontend (`drc-app`)

- **Language & UI:** Java 17, XML Layouts.
- **Architecture:** MVVM + Clean Architecture.
- **Core Libraries:** Retrofit2, OkHttp3, Room Database, Dagger-Hilt.
- **Mapping & Real-time:** Google Maps SDK, Google Location Services, Firebase Cloud Messaging (FCM).

### Backend (`spring-backend`)

- **Framework:** Spring Boot 4.x (Java 25).
- **Database:** Supabase (PostgreSQL).
- **Geospatial Engine:** **PostGIS** (via Hibernate Spatial) for blazing-fast radius queries.
- **Security:** Spring Security + JWT (BCrypt).
- **Real-time:** Spring WebSocket (STOMP).

## 🧠 Core Algorithms

### 1. Smart Dispatch Strategies (Strategy Pattern)

- **Emergency SOS (Life-threatening):** Broadcasts the mission to the Top 10 nearest volunteers simultaneously. First to accept gets the mission (Race condition handled via **Redis**).
- **Supply Delivery:** Sequential batches. System notifies the #1 prioritized volunteer with a 15-second exclusive window. If ignored, it falls back to a broadcast for the next group.

### 2. Volunteer Priority Score

The system calculates the suitability of a volunteer using the following formula:

$$S = (D \times 40\\%) + (R \times 20\\%) + (T \times 15\\%) + (A \times 15\\%) + (E \times 10\\%)$$

Where:

- **D (Distance):** Proximity to the hub/victim.
- **R (Rating):** Trust score from previous rescues.
- **T (Tasks):** Total completed tasks.
- **A (Average Response):** Speed of accepting tasks.
- **E (Experience):** Familiarity with the specific geographical area.

## 🚀 Getting Started

### Prerequisites

- JDK 25
- Android Studio (Latest Version)
- A Supabase account (for PostgreSQL + PostGIS)
- Google Maps API Key

### Setup Backend

- Navigate to `spring-backend/resources/`.
- Copy `application-local.example.yaml` to `application-local.yaml`.
- Fill in your Supabase DB credentials and JWT secret in `application-local.yaml`.
- Run the Spring Boot application using your IDE.

### Setup Frontend

- Open `drc-app` in Android Studio.
- Insert your Google Maps API Key in the `local.properties` file or `AndroidManifest.xml`.
- Insert your Firebase `google-services.json` file into `/drc-app/app`.
- Sync Gradle and build the project on an Emulator or physical device.
