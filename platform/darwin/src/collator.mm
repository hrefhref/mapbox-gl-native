#include <mbgl/style/expression/collator.hpp>

#import <Foundation/Foundation.h>

namespace mbgl {
namespace style {
namespace expression {
    
class Collator::Impl {
public:
    Impl(bool caseSensitive, bool diacriticSensitive, optional<std::string> locale_)
    : options(caseSensitive ? NSCaseInsensitiveSearch : 0 |
              diacriticSensitive ? NSDiacriticInsensitiveSearch : 0)
    , locale(locale_ ?
                [[NSLocale alloc] initWithLocaleIdentifier:[NSString stringWithUTF8String:locale_->c_str()]] :
                [NSLocale currentLocale])
    {}
    
    bool operator==(const Impl& other) const {
        return options == other.options &&
            [[locale localeIdentifier] isEqualToString:[other.locale localeIdentifier]];
    }
    
    int compare(const std::string& lhs, const std::string& rhs) const {
        NSString* nsLhs = [NSString stringWithUTF8String:lhs.c_str()];
        // TODO: verify "abc" != "abcde" -- the "range" argument seems strange to me
        // https://developer.apple.com/documentation/foundation/nsstring/1414561-compare
        
        return [nsLhs compare:[NSString stringWithUTF8String:rhs.c_str()] options:options range:NSMakeRange(0, nsLhs.length) locale:locale];
    }
    
    std::string resolvedLocale() const {
        return [locale localeIdentifier].UTF8String;
    }
private:
    NSStringCompareOptions options;
    NSLocale* locale;
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
