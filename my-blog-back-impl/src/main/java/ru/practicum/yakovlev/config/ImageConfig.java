package ru.practicum.yakovlev.config;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ImageConfig {

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public AllowedImageTypes allowedImageTypes(
            @Value("${blog.image.allowed-types}") String configuredTypes
    ) {
        Set<String> values = Arrays.stream(configuredTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new AllowedImageTypes(values);
    }

}
