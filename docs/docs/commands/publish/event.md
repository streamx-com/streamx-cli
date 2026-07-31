---
title: "streamx publish event"
sidebar_label: "event"
sidebar_position: 1
description: "Publish a single event"
---

# `streamx publish event`

Publish a single event

```bash
streamx publish event [options] <templateId> <eventPayloadPath> [<eventSubject>]
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<templateId>` | yes | Template ID (the template to use for this event). Resolved from the context's event-templates folder and the shared &lt;streamx-home&gt;/default-event-templates (~/.streamx by default). Run `streamx settings event-templates list` to see all available templates. |
| `<eventPayloadPath>` | yes | Payload path |
| `<eventSubject>` | no | Event subject |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--auth-token`, `-a` | `<authToken>` | Authentication token Falls back to settings property: streamx.ingestion.auth-token |
| `--ingestion-url`, `-u` | `<url>` | StreamX ingestion URL Falls back to settings property: streamx.ingestion.url |
| `--insecure`, `-k` |  | Skip TLS verification Falls back to settings property: streamx.ingestion.insecure |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
