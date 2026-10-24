package com.vynk.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void appliesDocumentedDefaultsWhenOmitted() {
        PageQuery query = new PageQuery(null, null);

        assertThat(query.pageNumber()).isZero();
        assertThat(query.pageSize()).isEqualTo(20);
    }

    @Test
    void keepsExplicitValues() {
        PageQuery query = new PageQuery(3, 50);

        assertThat(query.pageNumber()).isEqualTo(3);
        assertThat(query.pageSize()).isEqualTo(50);
    }

    @Test
    void acceptsMaximumPageSize() {
        assertThat(new PageQuery(0, PageQuery.MAX_SIZE).pageSize()).isEqualTo(100);
    }
}
