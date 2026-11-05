# Gson reads and writes DTOs by field name, so R8 must not rename them.
-keep class com.fitquest.app.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# TypeToken subclasses in LocalCache need their generic signatures at runtime.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Credential Manager loads its Play Services provider by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
