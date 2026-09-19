# kotlinx.serialization 的序列化器靠反射查找，别混淆掉
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.wobok.bibilili.** {
    *** Companion;
}
-keepclasseswithmembers class com.wobok.bibilili.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.wobok.bibilili.**$$serializer { *; }

# Retrofit 接口方法的泛型签名不能丢
-keepattributes Signature, Exceptions
-keep,allowobfuscation interface com.wobok.bibilili.data.api.**
-keep,allowobfuscation interface com.wobok.bibilili.data.auth.AuthApi
