#include <mbgl/style/expression/collator.hpp>

// TODO: This is the Collator implementation for Qt when WITH_QT_I18N flag is set
// ie we can't link directly to ICU. Since Qt doesn't expose what we need to implement
// Collator, we have to figure out what to do here (maybe pass through to default string
// comparison?)

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
