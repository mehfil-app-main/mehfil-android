# Firestore documents are mapped by hand (Mapping.kt), so no model class is reachable by reflection and no
# -keep rule is needed for them. If that ever changes to reflective (de)serialisation, the model package must
# be kept or release builds will silently read back empty documents.
-keepattributes Signature,*Annotation*

# slf4j is pulled in transitively by a Firebase dependency but never used at runtime.
-dontwarn org.slf4j.**

# Strip diagnostic logging from release builds. Log.w and Log.e are kept deliberately: they are the only
# record of a failed write, and they carry no user content (see AppStore.report).
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
