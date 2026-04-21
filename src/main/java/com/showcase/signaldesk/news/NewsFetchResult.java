package com.showcase.signaldesk.news;

import java.util.List;

public record NewsFetchResult(
    List<NewsItem> items,
    boolean live,
    String message
) {
}
