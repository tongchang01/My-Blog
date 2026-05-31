package com.tyb.myblog.v2.common.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {

    @Test
    void replacesNullRecordsWithEmptyList() {
        PageResponse<String> response = new PageResponse<>(null, 0, 1, 10);

        assertThat(response.records()).isEmpty();
    }

    @Test
    void copiesRecordsDefensively() {
        PageResponse<String> response = new PageResponse<>(List.of("a"), 1, 1, 10);

        assertThat(response.records()).containsExactly("a");
        assertThatThrownBy(() -> response.records().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidMetadata() {
        assertThatThrownBy(() -> new PageResponse<>(List.of(), -1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
