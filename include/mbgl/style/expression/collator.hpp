#pragma once

#include <mbgl/util/feature.hpp>
#include <mbgl/util/optional.hpp>

#include <string>
#include <memory>

namespace mbgl {
namespace style {
namespace expression {

class Collator {
public:
    Collator(bool caseSensitive, bool diacriticSensitive, optional<std::string> locale = {});

    bool operator==(const Collator& other) const;

    int compare(const std::string& lhs, const std::string& rhs) const;

    std::string resolvedLocale() const;

    // TODO: This serialization shouldn't ever be used, but since we're part of
    // mbgl::style::expression::Value we're expected to have a serialize()
    mbgl::Value serialize() const;

private:
    class Impl;
    // TODO: Figure out right copy semantics for Collator -- sharing an underlying implementation
    // should be fine within one thread, but it might be more idiomatic to explicitly copy the
    // implementation?
    std::shared_ptr<Impl> impl;
};

} // namespace expression
} // namespace style
} // namespace mbgl
