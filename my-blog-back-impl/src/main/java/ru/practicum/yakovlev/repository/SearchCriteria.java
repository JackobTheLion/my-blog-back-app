package ru.practicum.yakovlev.repository;

import java.util.LinkedList;

public record SearchCriteria(
        String titlePart,
        LinkedList<String> tags
) {
}
