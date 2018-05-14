#pragma once

#include <mbgl/util/image.hpp>

#include <jni/jni.hpp>

/*
    android::Collator is the JNI wrapper of
    java/text/LocalGlyphRasterizer and java/text/Locale

    // TODO: Since these aren't mapbox specific, should
    // Collator/Locale wrappers be promoted to stand alone in
    // android/src similar to bitmap.hpp?

    mbgl::Collator is the portable interface
    Both implementations are in collator.cpp
 */

namespace mbgl {
namespace android {

class Locale {
public:
    static constexpr auto Name() { return "java/text/Locale"; };

    static jni::Object<Locale> forLanguageTag(jni::JNIEnv&, jni::String);

    static jni::String toLanguageTag(jni::JNIEnv&, jni::Object<Locale>);

    static jni::Class<Locale> javaClass;

    static void registerNative(jni::JNIEnv&);

};

class Collator {
public:
    static constexpr auto Name() { return "java/text/Collator"; };

    static jni::Object<Collator> getInstance(jni::JNIEnv&, jni::Object<Locale>);

    static void setStrength(jni::JNIEnv&, jni::Object<Collator>, jni::jint);

    static jni::jint compare(jni::JNIEnv&, jni::Object<Collator>, jni::String, jni::String);

    static jni::Class<Collator> javaClass;

    static void registerNative(jni::JNIEnv&);

};

} // namespace android
} // namespace mbgl
