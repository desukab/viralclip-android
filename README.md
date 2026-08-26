# ⚡ ViralClip — AI Video Repurposing for Android

> Transform long videos into viral short-form content — **100% on-device, 100% free**.

![License](https://img.shields.io/badge/license-MIT-blue)
![API](https://img.shields.io/badge/API-26%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blueviolet)
![Jetpack Compose](https://img.shields.io/badge/Compose-1.5-orange)

---

## 🎯 What is ViralClip?

ViralClip is a **professional-grade Android app** that replicates [Opus.pro](https://www.opus.pro/)'s core features entirely on-device. Content creators can:

- **Auto-detect viral moments** in long videos with AI scoring
- **Generate animated captions** with 15+ style presets
- **Reframe & resize** videos for any social platform
- **Track faces** for smart speaker-centered cropping
- **Export directly** to TikTok, YouTube Shorts, Instagram Reels, and more

**No cloud. No subscriptions. No watermarks. Your device, your content.**

---

## ✨ Features

### 🧠 AI-Powered Clip Detection
- **Virality Scoring** — Multi-dimensional analysis (hook strength, engagement, emotional impact, shareability, watch time)
- **Smart Segment Selection** — Finds the best moments using audio dynamics, visual engagement, and scene analysis
- **Scene Change Detection** — Identifies cuts and transitions automatically
- **Frame-by-Frame Analysis** — Brightness, motion, and visual variety metrics

### 🎬 Video Processing
- **Lossless Trimming** — MediaExtractor + MediaMuxer for frame-accurate cuts
- **Video Splitting** — Split clips at any point
- **Speed Control** — 0.25x to 3x playback speed
- **Smart Resize** — Automatic reframing for 9:16, 1:1, 16:9, and custom aspect ratios
- **Face Tracking** — ML Kit face detection for speaker-centered cropping

### 📝 Auto Captions
- **15 Caption Presets** — Bold Highlight, Karaoke, Typewriter, Neon Glow, Retro Wave, and more
- **Word-Level Highlighting** — Animated word-by-word appearance
- **12 Language Support** — English, Spanish, French, German, Japanese, Korean, Chinese, and more
- **Full Transcript Editor** — Edit individual caption segments
- **Custom Styles** — Font size, color, highlight color, outline, shadow, position, animation

### 🎨 Brand & Templates
- **12 Built-in Templates** — Viral, Professional, Creative, Minimal, Bold, Neon, Cinematic, Retro, Playful, Elegant
- **Brand Presets** — Save custom colors, fonts, and styles for consistent branding
- **15 Filter Presets** — Vivid, Warm, Cool, Vintage, Noir, Cyberpunk, Teal & Orange, and more

### 📱 Platform Optimization
- **7 Platform Presets** — TikTok, Instagram Reels, YouTube Shorts, Facebook, Twitter/X, LinkedIn, Pinterest
- **Resolution-aware** — Auto-optimizes export settings per platform
- **Duration Limits** — Respects platform max duration guidelines

### 📤 Export & Share
- **Multiple Formats** — MP4, MOV, WebM
- **Quality Presets** — Low (720p), Medium, High (1080p), Ultra (1080p+)
- **FPS Control** — 24, 30, or 60 fps
- **Direct Share** — Share to any social app via Android share sheet

---

## 🏗️ Architecture

```
com.viralclip.app/
├── core/                    # Processing engines
│   ├── ai/                  # AI analysis (ViralityScorer, CaptionGenerator, FaceTracker)
│   ├── analysis/            # Frame analysis (FrameAnalyzer)
│   ├── audio/               # Audio processing (AudioProcessor)
│   └── video/               # Video processing (FFmpegProcessor)
├── data/                    # Data layer
│   ├── database/            # Room DB, entities, DAOs
│   ├── preferences/         # DataStore preferences
│   └── repository/          # Repository implementations
├── di/                      # Hilt dependency injection
├── domain/                  # Domain layer
│   ├── model/               # Data models (60+ classes)
│   └── repository/          # Repository interfaces
├── services/                # Background services
│   ├── VideoProcessingPipeline  # Orchestrates AI analysis stages
│   ├── VideoProcessingService   # Foreground service for processing
│   └── ExportService            # Foreground service for export
├── ui/                      # Presentation layer
│   ├── components/          # Reusable UI components
│   ├── navigation/          # Navigation graph
│   ├── screens/             # 9 feature screens
│   │   ├── home/            # Home dashboard with quick actions
│   │   ├── editor/          # Video editor with tools
│   │   ├── captions/        # Caption editor with style picker
│   │   ├── preview/         # Viral clip preview & scoring
│   │   ├── export/          # Export settings & sharing
│   │   ├── templates/       # Template library
│   │   ├── brand/           # Brand presets manager
│   │   ├── projects/        # Project library
│   │   └── settings/        # App settings
│   ├── theme/               # Material 3 dark theme
│   └── viewmodels/          # ViewModels (MVVM)
└── util/                    # Extensions & utilities
```

### Tech Stack
| Technology | Purpose |
|---|---|
| **Kotlin** | Primary language |
| **Jetpack Compose** | UI framework (Material 3) |
| **Hilt** | Dependency injection |
| **Room** | Local database |
| **DataStore** | Preferences |
| **Media3/ExoPlayer** | Video playback |
| **MediaExtractor/Muxer** | Video processing |
| **ML Kit** | Face detection |
| **Coroutines + Flow** | Async processing |
| **CameraX** | Camera capture |
| **Coil** | Image loading |
| **Lottie** | Animations |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Physical device recommended (for camera & processing performance)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/yourusername/ViralClip.git
cd ViralClip

# Build the project
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Import via Intent
ViralClip supports receiving videos from other apps. Share a video from any app and select ViralClip — it will automatically start processing.

---

## 💰 How to Monetize

Since this is **free and open-source**, here are ways content creators can benefit:

1. **Self-hosted** — Install on personal devices, no subscription needed
2. **White-label** — Rebrand and sell as a SaaS product
3. **Premium Features** — Add cloud processing, team collaboration, API access
4. **Template Marketplace** — Sell custom caption style templates
5. **Enterprise** — Offer on-premise deployment for companies

---

## 📊 Feature Comparison with Opus.pro

| Feature | Opus.pro | ViralClip |
|---|---|---|
| AI Virality Scoring | ✅ (Cloud) | ✅ (On-device) |
| Auto Captions | ✅ | ✅ |
| Face Tracking | ✅ | ✅ (ML Kit) |
| Platform Presets | ✅ | ✅ (7 platforms) |
| Templates | ✅ | ✅ (12 templates) |
| Brand Presets | ✅ | ✅ |
| Speed Control | ✅ | ✅ |
| Filters | ✅ | ✅ (15 presets) |
| Export Options | ✅ | ✅ (MP4/MOV/WebM) |
| **Price** | **$15-49/mo** | **FREE** |
| **Privacy** | Cloud processing | **100% on-device** |
| **Watermarks** | Free plan only | **Never** |

---

## 🛠️ Building for Production

```bash
# Release build
./gradlew assembleRelease

# Bundle for Play Store
./gradlew bundleRelease
```

### Signing
Create a `keystore.properties` file in the project root:
```properties
storeFile=path/to/keystore.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_password
```

---

## 📝 License

```
MIT License

Copyright (c) 2024 ViralClip

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 Built With

- [Android Jetpack](https://developer.android.com/jetpack) — Modern Android development
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Declarative UI
- [ML Kit](https://developers.google.com/ml-kit) — On-device ML
- [Room](https://developer.android.com/training/data-storage/room) — Database
- [Hilt](https://dagger.dev/hilt/) — DI

---

**Made with ❤️ for content creators who deserve professional tools without the professional price tag.**
