-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep interface com.gallbladderz.openkick.core.network.api.** { *; }

-keepattributes *Annotation*
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    static kotlinx.serialization.KSerializer serializer(...);
}


-dontwarn org.koin.**
