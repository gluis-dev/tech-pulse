package com.showcase.signaldesk.news;

import java.time.Instant;

public record NewsItem(
    String id,
    String title,
    String summary,
    String source,
    String category,
    String url,
    Instant publishedAt,
    double viralityScore
) {
}
