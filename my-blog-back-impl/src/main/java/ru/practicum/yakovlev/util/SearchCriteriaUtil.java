package ru.practicum.yakovlev.util;

import ru.practicum.yakovlev.repository.SearchCriteria;

import java.util.LinkedList;
import java.util.List;

public class SearchCriteriaUtil {

    private SearchCriteriaUtil() {
    }

    public static SearchCriteria getSearchCriteria(String searchRequest) {
        if (searchRequest == null || searchRequest.isBlank()) {
            return new SearchCriteria("", new LinkedList<>());
        }

        String[] split = searchRequest.trim().split("\\s+");
        List<String> titleParts = new LinkedList<>();
        LinkedList<String> tags = new LinkedList<>();
        for (String s : split) {
            if (s.startsWith("#")) {
                tags.add(s.toLowerCase());
            } else {
                titleParts.add(s);
            }
        }
        return new SearchCriteria(String.join(" ", titleParts), tags);
    }

}
