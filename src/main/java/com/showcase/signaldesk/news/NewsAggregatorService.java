package com.showcase.signaldesk.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class NewsAggregatorService {
    private static final Logger logger = LoggerFactory.getLogger(NewsAggregatorService.class);
    private static final String QUERY = "(technology OR AI OR startup OR software OR cybersecurity OR semiconductors OR cloud)";
    private static final String DOMAINS =
        "techcrunch.com,theverge.com,wired.com,arstechnica.com,engadget.com,thenextweb.com,venturebeat.com";
    private static final int MAX_ITEMS = 60;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final ConcurrentMap<String, CachedResult> cache = new ConcurrentHashMap<>();
    private volatile List<NewsItem> cachedLiveItems = List.of();

    public NewsAggregatorService(@Value("${newsapi.key:}") String apiKey) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public NewsFetchResult getNews(String window, String sort) {
        if (apiKey.isBlank()) {
            logger.warn("NewsAPI key is not configured. Expected environment variable NEWS_API_KEY or property newsapi.key.");
            return new NewsFetchResult(List.of(), false, "NewsAPI key is not configured on the backend.");
        }

        Instant cutoff = resolveCutoff(window);
        Comparator<NewsItem> comparator = resolveComparator(sort);
        String cacheKey = window.toLowerCase(Locale.ROOT) + ":" + sort.toLowerCase(Locale.ROOT);

        CachedResult cachedResult = cache.get(cacheKey);
        if (cachedResult != null && cachedResult.expiresAt().isAfter(Instant.now())) {
            return cachedResult.result();
        }

        ApiFetchOutcome fetchOutcome = fetchNewsApiArticles(window, sort);
        List<NewsItem> liveItems = fetchOutcome.items().stream()
            .filter(item -> item.publishedAt() != null && item.publishedAt().isAfter(cutoff))
            .filter(item -> isValidUrl(item.url()))
            .filter(item -> isProbablyArticle(item.url()))
            .sorted(comparator)
            .limit(MAX_ITEMS)
            .toList();

        NewsFetchResult result;
        if (!liveItems.isEmpty()) {
            cachedLiveItems = liveItems;
            result = new NewsFetchResult(liveItems, true, "Live articles loaded successfully.");
        } else {
            List<NewsItem> cachedItems = cachedLiveItems.stream()
                .filter(item -> item.publishedAt().isAfter(cutoff))
                .sorted(comparator)
                .toList();

            String fallbackMessage = fetchOutcome.message() == null || fetchOutcome.message().isBlank()
                ? "No live articles were returned by NewsAPI."
                : fetchOutcome.message();
            result = new NewsFetchResult(cachedItems, false, fallbackMessage);
        }

        cache.put(cacheKey, new CachedResult(result, Instant.now().plus(CACHE_TTL)));
        return result;
    }

    private ApiFetchOutcome fetchNewsApiArticles(String window, String sort) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(buildEverythingUrl(window, sort)))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "TechPulse/1.0")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            NewsApiResponse payload = objectMapper.readValue(response.body(), NewsApiResponse.class);
            if (response.statusCode() >= 400) {
                String message = payload != null && payload.message() != null
                    ? payload.message()
                    : "NewsAPI returned HTTP " + response.statusCode() + ".";
                logger.warn("NewsAPI request failed: status={} code={} message={}",
                    response.statusCode(),
                    payload == null ? null : payload.code(),
                    message);
                return new ApiFetchOutcome(List.of(), message);
            }

            if (payload == null || !"ok".equalsIgnoreCase(payload.status()) || payload.articles() == null) {
                String message = payload != null && payload.message() != null
                    ? payload.message()
                    : "NewsAPI returned an unexpected response.";
                logger.warn("NewsAPI returned no usable articles. code={} message={}",
                    payload == null ? null : payload.code(),
                    message);
                return new ApiFetchOutcome(List.of(), message);
            }

            List<NewsItem> items = payload.articles().stream()
                .map(this::toNewsItem)
                .filter(Objects::nonNull)
                .toList();
            logger.info("NewsAPI returned {} candidate articles for window={} sort={}", items.size(), window, sort);
            return new ApiFetchOutcome(items, items.isEmpty() ? "NewsAPI returned zero matching articles." : "ok");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("NewsAPI request was interrupted.", ex);
            return new ApiFetchOutcome(List.of(), "The NewsAPI request was interrupted.");
        } catch (IOException ex) {
            logger.warn("NewsAPI request failed due to an I/O error.", ex);
            return new ApiFetchOutcome(List.of(), "The NewsAPI request failed due to an I/O error.");
        }
    }

    private NewsItem toNewsItem(NewsApiArticle article) {
        if (article == null || article.title() == null || article.url() == null) {
            return null;
        }

        Instant publishedAt = parseInstant(article.publishedAt());
        if (publishedAt == null) {
            return null;
        }

        String cleanTitle = stripTrailingSource(article.title());
        String source = article.source() != null && article.source().name() != null && !article.source().name().isBlank()
            ? article.source().name().trim()
            : extractDomain(article.url());
        String summary = sanitizeSummary(article.description(), article.content(), source);
        double viralityScore = scoreItem(cleanTitle, summary, publishedAt, source, article.author());

        return new NewsItem(
            UUID.nameUUIDFromBytes((source + article.url()).getBytes()).toString(),
            cleanTitle,
            summary,
            source,
            "Tech Industry",
            article.url().trim(),
            publishedAt,
            Math.round(viralityScore * 10.0) / 10.0
        );
    }

    private Comparator<NewsItem> resolveComparator(String sort) {
        return switch (sort.toLowerCase(Locale.ROOT)) {
            case "latest" -> Comparator.comparing(NewsItem::publishedAt).reversed();
            case "source" -> Comparator.comparing(NewsItem::source)
                .thenComparing(NewsItem::publishedAt, Comparator.reverseOrder());
            case "viral" -> Comparator.comparing(NewsItem::viralityScore, Comparator.reverseOrder())
                .thenComparing(NewsItem::publishedAt, Comparator.reverseOrder());
            default -> Comparator.comparing(NewsItem::viralityScore, Comparator.reverseOrder());
        };
    }

    private Instant resolveCutoff(String window) {
        return switch (window.toLowerCase(Locale.ROOT)) {
            case "3d" -> Instant.now().minus(Duration.ofDays(3));
            case "7d" -> Instant.now().minus(Duration.ofDays(7));
            case "24h" -> Instant.now().minus(Duration.ofHours(24));
            default -> Instant.now().minus(Duration.ofHours(24));
        };
    }

    private double scoreItem(String title, String summary, Instant publishedAt, String source, String author) {
        String haystack = (title + " " + summary + " " + source + " " + (author == null ? "" : author))
            .toLowerCase(Locale.ROOT);
        double keywordBoost = 0.0;
        if (haystack.contains("ai") || haystack.contains("openai") || haystack.contains("gpu")) {
            keywordBoost += 18;
        }
        if (haystack.contains("apple") || haystack.contains("google") || haystack.contains("microsoft")
            || haystack.contains("meta") || haystack.contains("amazon") || haystack.contains("nvidia")
            || haystack.contains("startup") || haystack.contains("security")) {
            keywordBoost += 12;
        }

        long hoursOld = Math.max(1, Duration.between(publishedAt, Instant.now()).toHours());
        double recencyBoost = Math.max(14, 82 - (hoursOld * 2.1));
        double sourceBoost = sourceAuthority(source);

        return keywordBoost + recencyBoost + sourceBoost;
    }

    private String extractDomain(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "External Source";
            }
            return host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException ex) {
            return "External Source";
        }
    }

    private boolean isValidUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isProbablyArticle(String rawUrl) {
        String value = rawUrl.toLowerCase(Locale.ROOT);
        return !value.contains("/video/")
            && !value.endsWith(".xml")
            && !value.endsWith(".rss")
            && !value.contains("/tag/")
            && !value.contains("/category/");
    }

    private String buildEverythingUrl(String window, String sort) {
        String sortBy = switch (sort.toLowerCase(Locale.ROOT)) {
            case "latest", "source" -> "publishedAt";
            case "viral" -> "popularity";
            default -> "publishedAt";
        };

        Instant from = resolveCutoff(window);
        String fromValue = from.toString();

        try {
            return "https://newsapi.org/v2/everything"
                + "?q=" + URLEncoder.encode(QUERY, "UTF-8")
                + "&domains=" + URLEncoder.encode(DOMAINS, "UTF-8")
                + "&language=en"
                + "&sortBy=" + sortBy
                + "&pageSize=100"
                + "&from=" + URLEncoder.encode(fromValue, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 should always be available", ex);
        }
    }

    private Instant parseInstant(String rawValue) {
        try {
            return rawValue == null || rawValue.isBlank() ? null : Instant.parse(rawValue);
        } catch (Exception ex) {
            return null;
        }
    }

    private String sanitizeSummary(String description, String content, String source) {
        String candidate = firstNonBlank(description, content);
        if (candidate == null) {
            return "Real article link from " + source + ".";
        }

        String cleaned = candidate
            .replaceAll("\\s*\\[\\+\\d+ chars\\]\\s*$", "")
            .replaceAll("\\s+", " ")
            .trim();

        if (cleaned.isBlank()) {
            return "Real article link from " + source + ".";
        }

        return cleaned;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String stripTrailingSource(String title) {
        return title.replaceAll("\\s+-\\s+[^-]+$", "").trim();
    }

    private double sourceAuthority(String source) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if (normalized.contains("techcrunch") || normalized.contains("the verge") || normalized.contains("ars technica")) {
            return 16;
        }
        if (normalized.contains("wired") || normalized.contains("engadget") || normalized.contains("venturebeat")) {
            return 12;
        }
        return 8;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsApiResponse(
        String status,
        String code,
        String message,
        List<NewsApiArticle> articles
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsApiArticle(
        NewsApiSource source,
        String author,
        String title,
        String description,
        String url,
        String content,
        String publishedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsApiSource(
        String id,
        String name
    ) {
    }

    private record CachedResult(
        NewsFetchResult result,
        Instant expiresAt
    ) {
    }

    private record ApiFetchOutcome(
        List<NewsItem> items,
        String message
    ) {
    }
}
