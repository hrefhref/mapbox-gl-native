#include <mbgl/style/expression/collator.hpp>

// TODO: Currently a stub, but should hook up to a mason-bundled ICU for
// collator functionality. Only used on platforms that don't have natively
// exposed collators, because the bundling is expensive.

namespace mbgl {
namespace style {
namespace expression {

class Collator::Impl {
public:
    Impl(bool, bool, optional<std::string>)
    {}

    bool operator==(const Impl&) const {
        return false;
    }

    int compare(const std::string&, const std::string&) const {
        return 0;
    }

    std::string resolvedLocale() const {
        return "";
    }
private:
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
