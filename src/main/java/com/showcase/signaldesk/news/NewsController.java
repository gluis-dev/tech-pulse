package com.showcase.signaldesk.news;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsAggregatorService newsAggregatorService;

    public NewsController(NewsAggregatorService newsAggregatorService) {
        this.newsAggregatorService = newsAggregatorService;
    }

    @GetMapping
    public NewsResponse getNews(
        @RequestParam(defaultValue = "24h") String window,
        @RequestParam(defaultValue = "viral") String sort
    ) {
        NewsFetchResult result = newsAggregatorService.getNews(window, sort);
        return new NewsResponse(
            result.items(),
            result.items().size(),
            window,
            sort,
            result.live(),
            result.message()
        );
    }
}
