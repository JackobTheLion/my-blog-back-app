package ru.practicum.yakovlev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.yakovlev.repository.SearchCriteria;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchCriteriaUtilTest {

    @Test
    @DisplayName("Returns empty criteria for null and blank requests")
    void shouldReturnEmptyCriteriaForNullAndBlankRequests() {
        assertEquals(new SearchCriteria("", new java.util.LinkedList<>()), SearchCriteriaUtil.getSearchCriteria(null));
        assertEquals(new SearchCriteria("", new java.util.LinkedList<>()), SearchCriteriaUtil.getSearchCriteria("   \t"));
    }

    @Test
    @DisplayName("Preserves title word spacing and normalizes tags")
    void shouldPreserveTitleWordSpacingAndNormalizeTags() {
        SearchCriteria result = SearchCriteriaUtil.getSearchCriteria("  Spring   #JAVA  JDBC  #Web ");

        assertEquals("Spring JDBC", result.titlePart());
        assertEquals(List.of("#java", "#web"), result.tags());
    }
}
