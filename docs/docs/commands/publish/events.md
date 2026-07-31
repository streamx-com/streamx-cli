---
title: "streamx publish events"
sidebar_label: "events"
sidebar_position: 2
description: "Publishes multiple events based on a directory structure and .eventtemplate files"
---

# `streamx publish events`

Publishes multiple events based on a directory structure and .eventtemplate files

```bash
streamx publish events [options] <path>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<path>` | yes | Path to directory tree with .eventtemplate files |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--auth-token`, `-a` | `<authToken>` | Authentication token Falls back to settings property: streamx.ingestion.auth-token |
| `--batch-size`, `-b` | `<batchSize>` | Publish events in batches if &gt; 1. Per-event error reporting is omitted |
| `--continue-on-error`, `-x` |  | Continue even if some event publish failed |
| `--debug` |  | Publish events normally AND write the rendered artefacts (templates, patched templates, resolved events) to a temporary directory for inspection |
| `--dry-run` |  | Render all events (applying templates and patches) and write the results to a temporary directory for inspection, without publishing anything to StreamX |
| `--ingestion-url`, `-u` | `<url>` | StreamX ingestion URL Falls back to settings property: streamx.ingestion.url |
| `--insecure`, `-k` |  | Skip TLS verification Falls back to settings property: streamx.ingestion.insecure |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `--patch`, `-p` | `<patchName>` | Name of the patch to apply (.&#123;patchName&#125;.eventtemplate) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
