# 数据模型通过 java.io.Serializable 存入 Bundle（rememberSaveable），保持字段与类名
-keep class com.hrt.monitor.data.** implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable { <fields>; }
-keepattributes InnerClasses,EnclosingMethod,Signature
