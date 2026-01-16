# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Onyx is a self-hosted web interface for AWS S3 file management. It stores files in S3 and metadata in DynamoDB, with embedded Solr for full-text search. Built for Java 11, deploys as a single fat JAR.

## Build & Run Commands

```bash
# Build the project (outputs to dist/onyx-0.1-runnable.jar)
mvn package

# Run locally (requires ~/onyx-dev.conf or specify config path)
java -Dconfig.file=~/onyx-dev.conf -jar dist/onyx-0.1-runnable.jar

# Run tests only
mvn test

# Clean build
mvn clean package
```

## Architecture

### Key Technologies
- **Java 11** with Jetty 9.4.x web server
- **Curacao** - lightweight web framework (custom, from markkolich.github.io/repo)
- **AWS SDK v1** - S3, DynamoDB, SNS
- **Apache Solr 8.11.1** - embedded search
- **FreeMarker** - HTML templating
- **Quartz** - scheduled jobs

### Directory Structure
```
src/main/java/onyx/
├── Application.java          # Entry point, Jetty server setup
├── controllers/
│   ├── api/                  # REST API endpoints (/api/v1/*)
│   └── *.java                # Web UI controllers (FreeMarker views)
├── components/
│   ├── aws/                  # AWS SDK wrappers (S3, DynamoDB, SNS)
│   ├── storage/              # ResourceManager, AssetManager, CacheManager
│   ├── search/               # Solr integration
│   └── authentication/       # Session, user auth, 2FA
├── entities/                 # Data models (Resource, etc.)
└── exceptions/               # Custom exception types

src/main/webapp/
├── static/                   # CSS, JS, images (Gulp build pipeline)
└── templates/                # FreeMarker .ftl templates

src/main/resources/
├── application.conf          # Default HOCON configuration
└── logback.xml               # Logging config
```

### Key Patterns
- **Presigned URLs**: File uploads/downloads go directly to S3, bypassing the web server
- **Resource entity**: Central model with `path` (hash key) and `parent` (range key) in DynamoDB
- **Visibility model**: Resources are PUBLIC or PRIVATE; private requires authentication
- **Async thread pools**: Separate pools for resources, cache, and assets

## Configuration

Uses HOCON (Lightbend/Typesafe Config). Key config sections:
- `onyx.session` - auth settings, user credentials (bcrypt hashed)
- `onyx.aws` - credentials, S3 bucket, DynamoDB table
- `onyx.search` - Solr configuration
- `onyx.cache` - local file caching

## Code Style

- Checkstyle enforced (config in `build/checkstyle/config.xml`)
- PMD static analysis (config in `build/pmd/ruleset.xml`)
- Compiler warnings treated as errors (`-Werror`)
- All code must pass `mvn package` which runs checkstyle, PMD, and duplicate-finder

## Testing

- JUnit 5 + Mockito
- Tests in `src/test/java/`
- Run with `mvn test`
- Tests run in `America/Los_Angeles` timezone

## Frontend Build

The static assets use a Gulp pipeline:
- Source: `src/main/webapp/static/`
- Built automatically during `mvn package` via frontend-maven-plugin
- Node v8.17.0 / npm 6.4.1 installed locally by Maven
