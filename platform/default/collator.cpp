#include <mbgl/style/expression/collator.hpp>

// TODO: Currently a stub, but should hook up to a mason-bundled ICU for
// collator functionality. Only used on platforms that don't have natively
// exposed collators, because the bundling is expensive.

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

const std::string& Collator::resolvedLocale() const {
    static std::string placeholder;
    return placeholder;
}
mbgl::Value Collator::serialize() const {
    return mbgl::Value(true);
}


} // namespace expression
} // namespace style
} // namespace mbgl
