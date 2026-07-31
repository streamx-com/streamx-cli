---
title: "streamx settings event-templates list"
sidebar_label: "list"
sidebar_position: 6
description: "Display all available event templates"
---

# `streamx settings event-templates list`

Display all available event templates

```bash
streamx settings event-templates list [options]
```

Lists every template that `publish event` can resolve, along with its CloudEvent type, source (default / custom / registered in settings), and path on disk.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
