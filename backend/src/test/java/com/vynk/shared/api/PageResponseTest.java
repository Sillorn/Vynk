package com.vynk.shared.api;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {

    @Test
    void derivesTotalPagesAndHasNext() {
        PageResponse<String> first = PageResponse.of(List.of("a", "b"), 0, 2, 5);

        assertThat(first.items()).containsExactly("a", "b");
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
    }

    @Test
    void marksLastPageWithoutNext() {
        PageResponse<String> last = PageResponse.of(List.of("e"), 2, 2, 5);

        assertThat(last.totalPages()).isEqualTo(3);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void handlesEmptyResult() {
        PageResponse<String> empty = PageResponse.of(List.of(), 0, 20, 0);

        assertThat(empty.items()).isEmpty();
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.hasNext()).isFalse();
    }

    @Test
    void defensivelyCopiesItems() {
        List<String> source = new java.util.ArrayList<>(List.of("a"));
        PageResponse<String> page = PageResponse.of(source, 0, 20, 1);

        source.add("b");

        assertThat(page.items()).containsExactly("a");
    }

    @Test
    void rejectsOutOfRangeMetadata() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 101, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
