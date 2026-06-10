# Live Coverage Release Strategy

Paid plugin versioning follows [JetBrains Marketplace rules](https://plugins.jetbrains.com/docs/marketplace/add-required-parameters.html).

## Current major release (2026.2)

| Field | Value | Where |
|-------|-------|-------|
| Plugin version | `2026.2.0` | `build.gradle.kts`, `plugin.xml` |
| Product code | `PLIVECOVERAGE` | `plugin.xml` `product-descriptor` |
| Release version | `20262` | `plugin.xml` `product-descriptor` |
| Release date | `20260203` | `plugin.xml` `product-descriptor` |

## Minor updates (bug fixes, small improvements)

Stay on the same major release. Change **only** the plugin version patch segment:

- `2026.2.0` → `2026.2.1` → `2026.2.2`
- Keep `release-version="20262"` and `release-date="20260203"` unchanged
- Update `changeNotes` in `build.gradle.kts`

## Major updates (new paid major version)

Start a new major line when you ship breaking changes or a new paid major:

1. Set plugin version, e.g. `2026.3.0`
2. Update `release-version`, e.g. `20263`
3. Set `release-date` to the publication date (`YYYYMMDD`)
4. Update `changeNotes`

Changing `release-version` / `release-date` resets active trial licenses for that product.

## Marketplace checklist per release

1. Bump version in `build.gradle.kts` and `plugin.xml`
2. Update `changeNotes` in `build.gradle.kts`
3. Run `./gradlew buildPlugin`
4. Upload the ZIP from `build/distributions/`

Trial (30 days) and monthly subscription are configured in JetBrains Sales System, not in this repository.
