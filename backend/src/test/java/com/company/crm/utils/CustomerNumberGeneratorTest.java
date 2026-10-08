package com.company.crm.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNumberGeneratorTest {

    @Test
    void shouldGenerateUniqueNumbersConcurrentlyWithinColumnLength() {
        CustomerNumberGenerator generator = new CustomerNumberGenerator();

        Set<String> numbers = IntStream.range(0, 2_000)
                .parallel()
                .mapToObj(ignored -> generator.customerNo(generator.nextId()))
                .collect(Collectors.toSet());

        assertThat(numbers).hasSize(2_000)
                .allMatch(number -> number.startsWith("KH") && number.length() <= 32);
    }
}
