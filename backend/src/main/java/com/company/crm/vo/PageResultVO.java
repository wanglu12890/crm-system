package com.company.crm.vo;

import java.util.List;

/** Minimal API pagination contract independent of MyBatis-Plus implementation details. */
public record PageResultVO<T>(
        List<T> records,
        long total,
        long page,
        long size,
        long pages
) {
}
