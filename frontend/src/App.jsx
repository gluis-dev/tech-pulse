import { useEffect, useState } from "react";

const windows = [
  { label: "Last 24h", value: "24h" },
  { label: "Past 3 days", value: "3d" },
  { label: "Past week", value: "7d" }
];

const sorts = [
  { label: "Virality", value: "viral" },
  { label: "Latest", value: "latest" },
  { label: "Source", value: "source" }
];

const sourceColors = [
  "var(--signal-1)",
  "var(--signal-2)",
  "var(--signal-3)",
  "var(--signal-4)",
  "var(--signal-5)"
];

export default function App() {
  const [windowValue, setWindowValue] = useState("24h");
  const [sortValue, setSortValue] = useState("viral");
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    async function loadNews() {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(
          `${import.meta.env.BASE_URL}api/news?window=${windowValue}&sort=${sortValue}`,
          { signal: controller.signal }
        );

        if (!response.ok) {
          throw new Error("Unable to load the news feed.");
        }

        const payload = await response.json();
        const nextItems = payload.items ?? [];
        setItems(nextItems);
        setTotal(payload.total ?? nextItems.length);

        if (payload.live === false && nextItems.length === 0) {
          setError(payload.message || "No live articles are available right now.");
        }
      } catch (err) {
        if (err.name !== "AbortError") {
          setError("The news desk could not reach the feeds right now. Try refreshing in a moment.");
        }
      } finally {
        setLoading(false);
      }
    }

    loadNews();

    return () => controller.abort();
  }, [sortValue, windowValue]);

  const topStory = items[0];
  const averageScore = items.length
    ? (items.reduce((sum, item) => sum + item.viralityScore, 0) / items.length).toFixed(1)
    : "0.0";
  const uniqueSources = new Set(items.map((item) => item.source)).size;
  const topSources = buildTopSources(items);
  const latestTimestamp = topStory ? formatDate(topStory.publishedAt) : "Awaiting update";
  const trendLabel =
    sortValue === "viral" ? "Momentum-ranked" : sortValue === "latest" ? "Freshest-first" : "Publisher-sorted";
  const headlineItems = items.slice(0, 3);
  const gridItems = items.slice(3);
  const showingAll = total <= items.length;

  return (
    <div className="app-shell">
      <section className="hero">
        <div className="hero-card">
          <div className="eyebrow">Tech Industry Command Center</div>
          <h1 className="hero-title">Tech Pulse</h1>
          <p className="hero-copy">
            A focused news surface for tracking the stories shaping the tech industry. Scan the biggest
            developments, sort by momentum or freshness, and jump straight into the original reporting
            without leaving the signal behind.
          </p>
          <div className="hero-strip">
            <div className="strip-item">
              <div className="strip-label">Coverage</div>
              <div className="strip-value">Tech radar</div>
            </div>
            <div className="strip-item">
              <div className="strip-label">Active Lens</div>
              <div className="strip-value">{trendLabel}</div>
            </div>
            <div className="strip-item">
              <div className="strip-label">Latest Signal</div>
              <div className="strip-value">{latestTimestamp}</div>
            </div>
          </div>
        </div>

        <aside className="stats-card">
          <div>
            <div className="eyebrow">Live Snapshot</div>
            <div className="stats-grid">
              <div className="stat">
                <div className="stat-label">Stories matched</div>
                <div className="stat-value">{total}</div>
              </div>
              <div className="stat">
                <div className="stat-label">Shown right now</div>
                <div className="stat-value">{items.length}</div>
              </div>
              <div className="stat">
                <div className="stat-label">Average virality</div>
                <div className="stat-value">{averageScore}</div>
              </div>
            </div>
          </div>

          <div className="stats-footnote">
            {topStory
              ? `Top signal right now: ${topStory.source} on ${formatDate(topStory.publishedAt)}`
              : "Waiting for the first signal..."}
          </div>
        </aside>
      </section>

      <section className="toolbar">
        <div className="toolbar-controls">
          <div className="toolbar-block">
            <div className="toolbar-label">Time Window</div>
            <div className="toolbar-group">
              {windows.map((option) => (
                <button
                  key={option.value}
                  className={`chip ${windowValue === option.value ? "is-active" : ""}`}
                  onClick={() => setWindowValue(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="toolbar-divider" aria-hidden="true"></div>

          <div className="toolbar-block">
            <div className="toolbar-label">Ranking Mode</div>
            <div className="toolbar-group">
              {sorts.map((option) => (
                <button
                  key={option.value}
                  className={`chip ${sortValue === option.value ? "is-active" : ""}`}
                  onClick={() => setSortValue(option.value)}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="toolbar-summary">
          <div className="toolbar-summary-label">Live Coverage</div>
          <div className="toolbar-summary-value">
            {total ? `${total} matched articles` : "Waiting for stories"}
          </div>
          <div className="toolbar-summary-meta">
            {total
              ? showingAll
                ? `${uniqueSources} active sources in view`
                : `Showing top ${items.length} from ${uniqueSources} visible sources`
              : "Fetching the latest feed"}
          </div>
        </div>
      </section>

      {loading ? (
        <div className="loader">Pulling the latest stories from across the tech industry...</div>
      ) : error ? (
        <div className="empty-state">{error}</div>
      ) : items.length === 0 ? (
        <div className="empty-state">No stories matched this time window.</div>
      ) : (
        <>
          <section className="content-grid">
            <div className="lead-column">
              {headlineItems.map((item, index) => (
                <article key={item.id} className={`news-card feature-card feature-card-${index + 1}`}>
                  <div className="feature-rank">{`0${index + 1}`}</div>
                  <div>
                    <div className="card-topline">
                      <div className="card-source">
                        <span
                          className="dot"
                          style={{ background: sourceColors[index % sourceColors.length] }}
                        ></span>
                        <span>{item.source}</span>
                      </div>
                      <span>{formatRelativeTime(item.publishedAt)}</span>
                    </div>
                    <h2 className="feature-title">{item.title}</h2>
                    <p className="card-summary">{truncate(item.summary, index === 0 ? 220 : 170)}</p>
                  </div>

                  <div className="card-footer">
                    <div className="meta-cluster">
                      <div className="score-pill">Score {item.viralityScore}</div>
                      <div className="meta-pill">{item.category}</div>
                    </div>
                    <a className="read-link" href={item.url} target="_blank" rel="noreferrer">
                      Read story
                    </a>
                  </div>
                </article>
              ))}
            </div>

            <aside className="signal-rail">
              <div className="rail-card">
                <div className="rail-header">
                  <div className="eyebrow">Source Mix</div>
                  <div className="rail-title">Where the signal is coming from</div>
                </div>
                <div className="source-list">
                  {topSources.map((entry, index) => (
                    <div key={entry.name} className="source-row">
                      <div className="source-name-wrap">
                        <span
                          className="source-swatch"
                          style={{ background: sourceColors[index % sourceColors.length] }}
                        ></span>
                        <span className="source-name">{entry.name}</span>
                      </div>
                      <span className="source-count">{entry.count}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="rail-card rail-card-accent">
                <div className="eyebrow">Scanning Notes</div>
                <div className="rail-copy">
                  Sort by virality to surface stories from bigger publishers first, or switch to latest
                  when you want the fastest pulse on what just landed.
                </div>
              </div>
            </aside>
          </section>

          <section className="news-grid">
            {gridItems.map((item, index) => (
              <article key={item.id} className="news-card">
                <div>
                  <div className="card-topline">
                    <div className="card-source">
                      <span
                        className="dot"
                        style={{ background: sourceColors[index % sourceColors.length] }}
                      ></span>
                      <span>{item.source}</span>
                    </div>
                    <span>{formatRelativeTime(item.publishedAt)}</span>
                  </div>
                  <h2 className="card-title">{item.title}</h2>
                  <p className="card-summary">{truncate(item.summary, 185)}</p>
                </div>

                <div className="card-footer">
                  <div className="meta-cluster">
                    <div className="score-pill">Score {item.viralityScore}</div>
                    <div className="meta-pill">{item.category}</div>
                  </div>
                  <a className="read-link" href={item.url} target="_blank" rel="noreferrer">
                    Read story
                  </a>
                </div>
              </article>
            ))}
          </section>
        </>
      )}
    </div>
  );
}

function formatDate(value) {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatRelativeTime(value) {
  const date = new Date(value);
  const hours = Math.max(1, Math.round((Date.now() - date.getTime()) / (1000 * 60 * 60)));

  if (hours < 24) {
    return `${hours}h ago`;
  }

  const days = Math.round(hours / 24);
  return `${days}d ago`;
}

function truncate(text, maxLength) {
  if (!text || text.length <= maxLength) {
    return text;
  }

  return `${text.slice(0, maxLength).trim()}...`;
}

function buildTopSources(items) {
  const counts = new Map();

  items.forEach((item) => {
    counts.set(item.source, (counts.get(item.source) ?? 0) + 1);
  });

  return [...counts.entries()]
    .sort((left, right) => right[1] - left[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name, count }));
}
