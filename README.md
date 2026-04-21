# Tech Pulse

Tech Pulse is a simple React + Java web app that pulls tech news into one central homepage and lets you sort by virality, freshness, or source.

## What it does

- Aggregates live stories from NewsAPI using server-side requests.
- Filters stories by `Last 24h`, `Past 3 days`, and `Past week`.
- Sorts by `Virality`, `Latest`, or `Source`.
- Presents everything on a single polished homepage that is easy to demo.
- Opens each `Read story` link as a real article URL returned by NewsAPI.

## Stack

- Frontend: React 18 with Vite.
- Backend: Java 21 with Spring Boot.
- Data source: NewsAPI `/v2/everything` with real article URLs.

## Project Structure

- `frontend/`: React source code, Vite config, and frontend package metadata.
- `src/main/java/`: Spring Boot application and backend API code.
- `src/main/resources/application.properties`: Spring Boot configuration.
- `target/classes/static/`: generated frontend build output served by Spring Boot at runtime.
- `pom.xml`: Maven configuration for the Java backend.
- `README.md`: setup and run instructions.

## Prerequisites

- Java 21
- Maven 3.9+
- A NewsAPI key

## Setup

Set the NewsAPI key as an environment variable before starting the app.

PowerShell:

```powershell
$env:NEWS_API_KEY="your-key-here"
```

Git Bash:

```bash
export NEWS_API_KEY="your-key-here"
```

Do not hardcode the key into the source code or commit it to version control.

## Run

Start the Spring Boot app:

```bash
mvn spring-boot:run
```

Then open [http://localhost:8080/tech-pulse](http://localhost:8080/tech-pulse).

## Notes

- The backend sends the NewsAPI key server-side using an HTTP header, so the browser never receives the key.
- The frontend is a real React app in the `frontend/` folder and Maven builds it automatically during `mvn spring-boot:run` and `mvn package`.
- The app caches results briefly to make filter and sort changes feel faster.
- If no stories appear, confirm the `NEWS_API_KEY` environment variable is exported in the same shell session where you run Maven.
- `target/`, `.m2repo/`, and `frontend/node_modules/` are generated locally and should not be treated as project source.
- The first Maven run may take longer because it installs frontend dependencies and builds the React app automatically.

## Why this works well for a showcase

This project is intentionally simple in scope, but it still demonstrates:

- Product thinking: one place for a focused subject area.
- AI-assisted building: taking an idea to a working app quickly.
- Practical usefulness: a homepage you could actually use every day.
