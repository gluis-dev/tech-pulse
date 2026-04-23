# Tech Pulse

Tech Pulse is a React + Java web app that brings tech-industry news into one clean homepage and lets you sort stories by momentum, freshness, or source.

## Overview

- Aggregates live stories from NewsAPI using server-side requests.
- Filters stories by `Last 24h`, `Past 3 days`, and `Past week`.
- Sorts by `Virality`, `Latest`, or `Source`.
- Presents everything on a single polished homepage that is easy to scan and easy to demo.
- Opens each `Read story` link as a real article URL returned by NewsAPI.

## Purpose

This project was built to show how a focused idea can be turned into a real working product quickly.

- Build a web app that brings a specific type of information into one central place.
- Create a legitimate `Java + React` sample project with a clear real-world use case.
- Use AI-assisted software development to move from idea, to implementation, to a usable product faster.
- Demonstrate practical product thinking through a simple but meaningful user experience.

## Stack

- Frontend: React 18 with Vite
- Backend: Java 21 with Spring Boot
- Data source: NewsAPI `/v2/everything` with real article URLs

## Project Structure

- `frontend/`: React source code, Vite config, and frontend package metadata
- `src/main/java/`: Spring Boot application and backend API code
- `src/main/resources/application.properties`: Spring Boot configuration
- `target/classes/static/`: generated frontend build output served by Spring Boot at runtime
- `pom.xml`: Maven configuration for the Java backend
- `README.md`: setup and run instructions

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
