# Ogden Basic — release R8
# Compose / 协程由 AGP 默认规则覆盖；此处只补项目特有保留项。

# Room 实体与 DAO（反射 / 生成代码）
-keep class com.example.ogdenkids.data.** { *; }
-dontwarn androidx.room.paging.**

# whisper JNI
-keep class com.example.ogdenkids.speech.WhisperEngine { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# EncryptedSharedPreferences / Tink
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# JSON 备份字段名由代码显式 put，无需 keep data class 名
