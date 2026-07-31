---
title: "streamx publish stream"
sidebar_label: "stream"
sidebar_position: 3
description: "Publishes stream of events"
---

# `streamx publish stream`

Publishes stream of events

```bash
streamx publish stream [options] [<source>]
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<source>` | no | Events source. It can be a file path or resource URI |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--auth-token`, `-a` | `<authToken>` | Authentication token Falls back to settings property: streamx.ingestion.auth-token |
| `--batch-size`, `-b` | `<batchSize>` | Publish events in batches if &gt; 1. Per-event error reporting is omitted |
| `--continue-on-error`, `-x` |  | Continue even if some event publish failed |
| `--ingestion-url`, `-u` | `<url>` | StreamX ingestion URL Falls back to settings property: streamx.ingestion.url |
| `--insecure`, `-k` |  | Skip TLS verification Falls back to settings property: streamx.ingestion.insecure |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
