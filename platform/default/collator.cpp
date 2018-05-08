#include <mbgl/style/expression/collator.hpp>

// TODO: Currently a stub, but should hook up to a mason-bundled ICU for
// collator functionality. Only used on platforms that don't have natively
// exposed collators, because the bundling is expensive.

namespace mbgl {
namespace style {
namespace expression {

Collator::Collator(bool , bool , const std::string& )
    {}

Collator::Collator(bool , bool )
    {}

bool Collator::operator==(const Collator& ) const {
    return true;
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
