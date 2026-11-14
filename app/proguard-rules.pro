# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.lapakhunian.app.**$$serializer { *; }
-keepclassmembers class com.lapakhunian.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.lapakhunian.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn org.slf4j.**
