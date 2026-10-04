-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-keep class org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardService { <init>(); }

# ML Kit 17.3.0 (firebase-components 16.1.0) consumer rule keeps ComponentRegistrar
# classes but not their no-arg constructors, which Firebase discovers reflectively via
# manifest metadata; R8 strips the constructors -> NoSuchMethodException -> NPE in
# BarcodeScanning.getClient(). Fixed upstream in firebase-components 19.0.0.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
