---
title: "streamx project list"
sidebar_label: "list"
sidebar_position: 5
description: "List projects in an organization"
---

# `streamx project list`

List projects in an organization

```bash
streamx project list [options]
```

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `-q`, `--quiet` |  | Only display project IDs, one per line (for piping to xargs) |
| `-v`, `--verbose` |  | Print debug information |
| `--wide` |  | Also show each project's clusters and repository (one extra request per project) |

---

Every command also accepts the [global options](../global-options.md).
