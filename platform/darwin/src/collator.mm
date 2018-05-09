#include <mbgl/style/expression/collator.hpp>

#import <Foundation/Foundation.h>

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

int Collator::compare(const std::string& lhs, const std::string& rhs) const {
    NSStringCompareOptions options =
        caseSensitive ? NSCaseInsensitiveSearch : 0 |
        diacriticSensitive ? NSDiacriticInsensitiveSearch : 0;
    
    NSString* nsLhs = [NSString stringWithUTF8String:lhs.c_str()];
    // TODO: verify "abc" != "abcde" -- the "range" argument seems strange to me
    // https://developer.apple.com/documentation/foundation/nsstring/1414561-compare
    
    NSLocale* nsLocale = locale ?
        [[NSLocale alloc] initWithLocaleIdentifier:[NSString stringWithUTF8String:locale->c_str()]] :
        [NSLocale currentLocale];
    
    return [nsLhs compare:[NSString stringWithUTF8String:rhs.c_str()] options:options range:NSMakeRange(0, nsLhs.length) locale:nsLocale];
}

std::string Collator::resolvedLocale() const {
    NSLocale* nsLocale = locale ?
        [[NSLocale alloc] initWithLocaleIdentifier:[NSString stringWithUTF8String:locale->c_str()]] :
        [NSLocale currentLocale];
    
    return [nsLocale localeIdentifier].UTF8String;
}
mbgl::Value Collator::serialize() const {
    return mbgl::Value(true);
}


} // namespace expression
} // namespace style
} // namespace mbgl
