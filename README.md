# SecondBrain

SecondBrain is an Android-first personal memory system designed to capture, understand, connect, retrieve, and eventually act on a user's information over time.

> You do not have to build your second brain. It builds itself while you live.

## Current milestone

**v0.1 — Memory Core**

The first milestone focuses on proving one thing: SecondBrain can reliably remember information and retrieve it with traceable sources.

### Included in the bootstrap

- Android app module using Kotlin and Jetpack Compose
- Clean Architecture-oriented package boundaries
- Core `Memory` and `RawCapture` models
- `MemoryRepository` domain contract
- Provider-independent `BrainModel` AI contract
- WorkManager dependency ready for ingestion jobs
- Android CI debug build workflow

## Technical baseline

- Android Gradle Plugin 9.3.1
- Gradle 9.5.0 in CI
- compileSdk / targetSdk 37
- minSdk 26
- Kotlin / Compose Compiler 2.4.10
- Jetpack Compose BOM 2026.08.00
- Java 17

## Architecture direction

```text
Presentation
    ↓
Domain
    ↓
Data

Capture → Ingestion → Memory → Retrieval → Answer
```

The app is intentionally local-first. Cloud AI and sync will be introduced behind abstractions so the memory model is not coupled to a single provider.

## Next implementation slice

1. Room persistence for raw captures and memories
2. Capture text flow
3. Memory timeline
4. Background ingestion worker
5. Basic local search
6. First Ask SecondBrain retrieval path

## Branch

Bootstrap work is being developed on `feat/bootstrap-v0.1`.
