---
title: "streamx project clusters list"
sidebar_label: "list"
sidebar_position: 3
description: "List clusters available to a project and whether they are enabled"
---

# `streamx project clusters list`

List clusters available to a project and whether they are enabled

```bash
streamx project clusters list [options]
```

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-q`, `--quiet` |  | Only display cluster IDs, one per line (for piping to xargs) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../global-options.md).
