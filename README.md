# OralAI - Real-Time AI Oral Cancer Margin & Anomaly Detection

**OralAI** is a complete end-to-end clinical AI platform designed for early oral cancer detection, risk assessment, and patient margin evaluation. It features a modern web portal, an Android mobile application, a YOLOv8 deep-learning inference service, and a Supabase backend database.

---

## 🌟 Key Features

- 🔬 **AI-Powered Image Analysis**: Uses YOLOv8 object detection model to identify anomalous oral tissue and evaluate risk probability.
- 🌐 **Modern Web Application**: HTML5/CSS3/JavaScript portal for clinical scan uploading, patient search & filtering, interactive report management, and user authentication.
- 📱 **Android Mobile Application**: Native Jetpack Compose app for on-the-go patient scanning, report generation, and PDF export.
- ⚡ **Supabase Backend**: Integrated authentication, `patients` and `reports` PostgreSQL database tables with SQL schema included.
- 🔒 **Secure Auth & Session Sync**: Supports Supabase Authentication with password resets, full name session tracking, and offline local caching.

---

## 📁 Repository Structure

```
oral-ai/
├── web/                 # Web Portal (HTML, CSS, Vanilla JS, Supabase JS SDK)
├── backend/             # Python FastAPI Backend (YOLOv8 PyTorch model inference)
├── android/             # Native Android App (Kotlin, Jetpack Compose, Supabase-kt)
├── README.md
└── .gitignore
```

---

## 🚀 Quick Start Guide

### 1. Database Setup (Supabase)
Run the SQL queries in [`web/supabase_schema.sql`](web/supabase_schema.sql) in your Supabase SQL Editor to initialize:
- `patients` table
- `reports` table
- Hackathon test data & Row Level Security configuration

### 2. Running the Web Portal
Start a local HTTP server inside the `web/` directory:
```bash
cd web
python -m http.server 8080
```
Open `http://localhost:8080` in your web browser.

### 3. Running the AI Backend API
Install dependencies and launch the FastAPI backend:
```bash
cd backend
pip install -r requirements.txt
python main.py
```
The API server will run at `http://localhost:8000`.

### 4. Running the Android Application
1. Open the `android/` directory in **Android Studio**.
2. Click **File ➔ Sync Project with Gradle Files**.
3. Select **Build ➔ Build Bundle(s) / APK(s) ➔ Build APK(s)** to compile the debug APK.

---

## 🛠️ Technology Stack

- **Frontend**: HTML5, CSS3, JavaScript (Vanilla), Lucide Icons
- **Mobile**: Kotlin, Jetpack Compose, Material3, Coroutines, Supabase-kt
- **AI/ML Model**: PyTorch, Ultralytics YOLOv8, OpenCV, FastAPI
- **Database & Auth**: Supabase (PostgreSQL, Auth, PostgREST API)

---

## 📜 License
Developed for Hackathon & Clinical Demonstration. All rights reserved.
