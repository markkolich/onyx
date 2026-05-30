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
- **Java 11** with Jetty 11.0.26 web server
- **Curacao 7.1.3** - lightweight web framework
- **AWS SDK v2** - S3, DynamoDB, SNS
- **Apache Solr 9.x** - embedded search
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

- JUnit 6 + Mockito
- Tests in `src/test/java/`
- Run with `mvn test`
- Tests run in `America/Los_Angeles` timezone

## Frontend Build

The static assets use a Gulp 5 pipeline managed by `frontend-maven-plugin` 2.0.0:
- Source: `src/main/webapp/static/`
- Built automatically during `mvn package` via `frontend-maven-plugin`
- Node v24.16.0 / npm 11.13.0 installed locally by Maven into `src/main/webapp/static/node/`
- Output: `build/app.{css,js}` (concatenated), `release/app.min.{css,js}` (minified + bannered)

### Gulp tasks
- `release` — full production build (clean → concat → minify → banner)
- `dev` — concat only (no minify) + watch for CSS/JS changes
- `eslint-js` — lint `js/onyx/**/*.js` via ESLint 10 (config: `eslint.config.js`)

### CSS pipeline
Concatenated in order: Font Awesome (vendor + bundled webfonts), Magnific Popup, Nunito font, SB Admin 2 (light + dark themes) → minified with `gulp-clean-css`.

### JS pipeline
Concatenated in order: jQuery, jQuery UI Widget, Bootstrap bundle, jQuery File Upload (core + process + validate), copy-to-clipboard, jQuery Easing, Magnific Popup, Keypress, Underscore, marked, DOMPurify, SB Admin 2, then onyx app sources → minified with `gulp-terser`.

### Onyx app JS sources (`js/onyx/`)
- `onyx.js` — core namespace/utilities
- `app/app.js` — main app init
- `app/file.js` — file operations
- `app/directory.js` — directory operations
- `app/shortlink.js` — short link generation
- `app/previewer.js` — file preview
- `app/webauthn.js` — WebAuthn / passkey support
- `app/widgets/dark-mode.js` — dark mode toggle
- `app/widgets/markdown.js` — Markdown rendering (marked + DOMPurify)
- `app/widgets/keyboard.js` — keyboard shortcuts

### Vendor libraries (`vendor/`)
Bootstrap, Chart.js, DataTables, DOMPurify, Font Awesome Free, jQuery, jQuery Easing, jQuery File Upload, jQuery UI Widget, Keypress, Magnific Popup, marked, Underscore
