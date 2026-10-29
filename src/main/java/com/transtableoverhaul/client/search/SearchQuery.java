package com.transtableoverhaul.client.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * AE2/EMI style search syntax.
 * <ul>
 *     <li>{@code word} - display name or item id contains word</li>
 *     <li>{@code @mod} - mod id or mod name contains mod</li>
 *     <li>{@code #tag} - any item tag contains tag</li>
 *     <li>{@code $text} - tooltip contains text</li>
 *     <li>{@code -term} - negates any of the above</li>
 *     <li>Space separated terms must all match, {@code |} separates alternatives</li>
 * </ul>
 */
public final class SearchQuery implements Predicate<SearchEntry> {

    public static final SearchQuery EMPTY = new SearchQuery(List.of(), false);

    private final List<List<Predicate<SearchEntry>>> alternatives;
    private final boolean needsTooltips;

    private SearchQuery(List<List<Predicate<SearchEntry>>> alternatives, boolean needsTooltips) {
        this.alternatives = alternatives;
        this.needsTooltips = needsTooltips;
    }

    public static SearchQuery parse(String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return EMPTY;
        }
        List<List<Predicate<SearchEntry>>> alternatives = new ArrayList<>();
        boolean needsTooltips = false;
        for (String alternative : query.split("\\|")) {
            List<Predicate<SearchEntry>> terms = new ArrayList<>();
            for (String raw : alternative.trim().split("\\s+")) {
                boolean negate = raw.startsWith("-");
                String term = negate ? raw.substring(1) : raw;
                if (term.isEmpty()) {
                    continue;
                }
                Predicate<SearchEntry> predicate;
                char prefix = term.charAt(0);
                String value = term.substring(1);
                if (prefix == '@') {
                    predicate = entry -> entry.modId().contains(value) || entry.modName().contains(value);
                } else if (prefix == '#') {
                    predicate = entry -> entry.tags().stream().anyMatch(tag -> tag.contains(value));
                } else if (prefix == '$') {
                    needsTooltips = true;
                    predicate = entry -> entry.tooltip().contains(value);
                } else {
                    predicate = entry -> entry.name().contains(term) || entry.id().contains(term);
                }
                terms.add(negate ? predicate.negate() : predicate);
            }
            if (!terms.isEmpty()) {
                alternatives.add(terms);
            }
        }
        return alternatives.isEmpty() ? EMPTY : new SearchQuery(alternatives, needsTooltips);
    }

    public boolean isEmpty() {
        return alternatives.isEmpty();
    }

    public boolean needsTooltips() {
        return needsTooltips;
    }

    @Override
    public boolean test(SearchEntry entry) {
        if (alternatives.isEmpty()) {
            return true;
        }
        for (List<Predicate<SearchEntry>> terms : alternatives) {
            if (terms.stream().allMatch(term -> term.test(entry))) {
                return true;
            }
        }
        return false;
    }
}
