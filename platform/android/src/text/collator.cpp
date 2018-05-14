#include <mbgl/style/expression/collator.hpp>
#include <mbgl/util/platform.hpp>

#include <jni/jni.hpp>

#include "../attach_env.hpp"
#include "collator_jni.hpp"

// TODO: This Android collator stub should hook up to
// https://developer.android.com/reference/java/text/Collator
// via JNI

namespace mbgl {
namespace android {

void Collator::registerNative(jni::JNIEnv& env) {
    javaClass = *jni::Class<Collator>::Find(env).NewGlobalRef(env).release();
}

jni::Class<Collator> Collator::javaClass;

jni::Object<Collator> Collator::getInstance(jni::JNIEnv& env, jni::Object<Locale> locale) {
    using Signature = jni::Object<Collator>(jni::Object<Locale>);
    auto method = javaClass.GetStaticMethod<Signature>(env, "getInstance");
    return javaClass.Call(env, method, locale);
}

void Collator::setStrength(jni::JNIEnv& env, jni::Object<Collator> collator, jni::jint strength) {
    using Signature = void(jni::jint);
    auto static method = javaClass.GetMethod<Signature>(env, "setStrength");
    collator.Call(env, method, strength);
}

jni::jint Collator::compare(jni::JNIEnv& env, jni::Object<Collator> collator, jni::String lhs, jni::String rhs) {
    using Signature = jni::jint(jni::String, jni::String);
    auto static method = javaClass.GetMethod<Signature>(env, "compare");
    return collator.Call(env, method, lhs, rhs);
}

void Locale::registerNative(jni::JNIEnv& env) {
    javaClass = *jni::Class<Locale>::Find(env).NewGlobalRef(env).release();
}

jni::Object<Locale> Locale::forLanguageTag(jni::JNIEnv& env, jni::String languageTag) {
    using Signature = jni::Object<Locale>(jni::String);
    auto method = javaClass.GetStaticMethod<Signature>(env, "forLanguageTag");
    return javaClass.Call(env, method, languageTag);
}

jni::String Locale::toLanguageTag(jni::JNIEnv& env, jni::Object<Locale> locale) {
    using Signature = jni::String();
    auto static method = javaClass.GetMethod<Signature>(env, "toLanguageTag");
    return locale.Call(env, method);
}

jni::Class<Locale> Locale::javaClass;

} // namespace android

namespace style {
namespace expression {

class Collator::Impl {
public:
    Impl(bool caseSensitive_, bool diacriticSensitive_, optional<std::string> locale_)
        : caseSensitive(caseSensitive_)
        , diacriticSensitive(diacriticSensitive_)
    {
        android::UniqueEnv env { android::AttachEnv() }; // TODO: How does it work to hold onto jni Objects long term?
        jni::String languageTag = jni::Make<jni::String>(*env, locale_ ? *locale_ : "");
        locale = android::Locale::forLanguageTag(*env, languageTag);
        collator = android::Collator::getInstance(*env, locale);
        if (!diacriticSensitive) {
            // Only look for "base letter" differences, we'll look at case independently
            android::Collator::setStrength(*env, collator, 0 /*PRIMARY*/);
        } else if (diacriticSensitive && !caseSensitive) {
            android::Collator::setStrength(*env, collator, 1 /*PRIMARY*/);
        } else if (diacriticSensitive && caseSensitive) {
            android::Collator::setStrength(*env, collator, 2 /*TERTIARY*/);
        }
        //  is tricky, no native support
    }

    bool operator==(const Impl& other) const {
        return caseSensitive == other.caseSensitive &&
                diacriticSensitive == other.diacriticSensitive &&
                resolvedLocale() == other.resolvedLocale();
    }

    int compare(const std::string& lhs, const std::string& rhs) const {
        android::UniqueEnv env { android::AttachEnv() };

        jni::String jlhs = jni::Make<jni::String>(*env, lhs);
        jni::String jrhs = jni::Make<jni::String>(*env, rhs);

        jni::jint result = android::Collator::compare(*env, collator, jlhs, jrhs);;
        if (!diacriticSensitive && caseSensitive) {
            // java.text.Collator doesn't support a diacritic-insensitive/case-sensitive collation
            // order, so we have to compromise a little here.
            // (1) We use platform::lowercase to isolate case differences from other differences,
            //     but it's not locale aware.
            // (2) If we detect a case-only difference, we know the result is non-zero, but we
            //     have to fall back to the base sort order, which _might_ pick up a diacritic
            //     difference that ideally we'd ignore.
            if (!result) {
                // We compared at PRIMARY so we know there's no base letter difference
                auto lowerLhs = platform::lowercase(lhs);
                auto lowerRhs = platform::lowercase(rhs);
                if (lowerLhs != lowerRhs) {
                    // Case-only difference, fall back to base sort order
                    result = lowerLhs < lowerRhs ? -1 : 1;
                }
            }

        }
        jni::DeleteLocalRef(*env, jlhs);
        jni::DeleteLocalRef(*env, jrhs);

        return result;
    }

    std::string resolvedLocale() const {
        android::UniqueEnv env { android::AttachEnv() };
        jni::String languageTag = android::Locale::toLanguageTag(*env, locale);
        std::string result = jni::Make<std::string>(*env, languageTag);
        jni::DeleteLocalRef(*env, languageTag);
        return result;
    }
private:
    jni::Object<android::Collator> collator;
    jni::Object<android::Locale> locale;
    bool caseSensitive;
    bool diacriticSensitive;
};


Collator::Collator(bool caseSensitive, bool diacriticSensitive, optional<std::string> locale_)
    : impl(std::make_unique<Impl>(caseSensitive, diacriticSensitive, std::move(locale_)))
{}

bool Collator::operator==(const Collator& other) const {
    return *impl == *(other.impl);
}

int Collator::compare(const std::string& lhs, const std::string& rhs) const {
    return impl->compare(lhs, rhs);
}

std::string Collator::resolvedLocale() const {
    return impl->resolvedLocale();
}

mbgl::Value Collator::serialize() const {
    return mbgl::Value(true);
}


} // namespace expression
} // namespace style
} // namespace mbgl
