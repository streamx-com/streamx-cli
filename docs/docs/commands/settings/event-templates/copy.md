---
title: "streamx settings event-templates copy"
sidebar_label: "copy"
sidebar_position: 1
description: "Copy an existing event template under a new ID"
---

# `streamx settings event-templates copy`

Copy an existing event template under a new ID

```bash
streamx settings event-templates copy [options] [<sourceId>] [<destId>]
```

Copies the resolved content of &lt;sourceId&gt; into the context's event-templates/&lt;destId&gt;.json. Works with templates from any source (default / custom / registered in settings). The copy always lands in the context's event-templates folder.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<sourceId>` | no | Source template ID to copy from (prompts if omitted) |
| `<destId>` | no | Destination template ID (prompts if omitted) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
