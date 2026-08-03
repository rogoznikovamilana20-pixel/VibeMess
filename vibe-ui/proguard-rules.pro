# Vibe UI keep rules for R8/minify

# Room entities are referenced by generated code
-keep class com.vibe.ui.data.db.entity.** { *; }
-keep class com.vibe.ui.data.db.** { *; }

# Foreground/background components referenced by the manifest
-keep class com.vibe.ui.data.mesh.MeshService { *; }
-keep class com.vibe.ui.service.VibeDownloadService { *; }

# Entry points loaded via ApplicationLoader reflection
-keep class com.vibe.ui.** { <init>(...); }

# Lottie parses JSON compositions at runtime (no class reflection), keep the API surface used via composition
-dontwarn com.airbnb.lottie.**

# Bouncy Castle — keep ML-KEM/ML-DSA provider classes and crypto engine internals
-keep class org.bouncycastle.pqc.crypto.mlkem.** { *; }
-keep class org.bouncycastle.pqc.crypto.dilithium.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.mlkem.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.MLDSA** { *; }
-keep class org.bouncycastle.jcajce.spec.KTSParameterSpec** { *; }
-keep class org.bouncycastle.jcajce.spec.KEMExtractSpec** { *; }
-dontwarn org.bouncycastle.pqc.**
