package com.showcase.signaldesk.news;

import java.util.List;

public record NewsResponse(
    List<NewsItem> items,
    int total,
    String window,
    String sort,
    boolean live,
    String message
) {
}
