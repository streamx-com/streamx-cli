---
title: "streamx info"
sidebar_label: "info"
sidebar_position: 4
description: "Show CLI, context and connectivity diagnostics"
---

# `streamx info`

Show CLI, context and connectivity diagnostics

```bash
streamx info [options]
```

Reports the CLI version, the active context and where it came from, the effective endpoint settings, the stored login, and probes the configured endpoints. Useful as the first thing to share when something does not work.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--check` |  | Exit with code 1 unless every configured endpoint probes healthy (UP) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](global-options.md).
