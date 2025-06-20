#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_io_selectedfew_batteryomni_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */,
        jint level,
        jstring status,
        jint voltage,
        jint temperature) {

    const char *statusStr = env->GetStringUTFChars(status, nullptr);

    // Format output string
    char buffer[256];
    snprintf(buffer, sizeof(buffer),
             "Battery Level: %d%%\nStatus: %s\nVoltage: %.2f V\nTemperature: %.1f °C",
             level,
             statusStr,
             voltage / 1000.0f,
             temperature / 10.0f);

    env->ReleaseStringUTFChars(status, statusStr);

    return env->NewStringUTF(buffer);
}
