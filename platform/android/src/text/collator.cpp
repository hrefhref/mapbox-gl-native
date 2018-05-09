#include <mbgl/style/expression/collator.hpp>

// TODO: This Android collator stub should hook up to
// https://developer.android.com/reference/java/text/Collator
// via JNI

namespace mbgl {
namespace style {
namespace expression {

Collator::Collator(bool caseSensitive_, bool diacriticSensitive_, optional<std::string> locale_)
    : caseSensitive(caseSensitive_)
    , diacriticSensitive(diacriticSensitive_)
    , locale(std::move(locale_))
{}

bool Collator::operator==(const Collator& other) const {
    return caseSensitive == other.caseSensitive &&
        diacriticSensitive == other.diacriticSensitive &&
        locale == other.locale;
}

int Collator::compare(const std::string&, const std::string&) const {
    return 0;
}

std::string Collator::resolvedLocale() const {
    static std::string placeholder;
    return placeholder;
}
mbgl::Value Collator::serialize() const {
    return mbgl::Value(true);
}


} // namespace expression
} // namespace style
} // namespace mbgl
