---
title: "streamx project repo ssh-key show"
sidebar_label: "show"
sidebar_position: 4
description: "Print the public key of the configured SSH deploy key"
---

# `streamx project repo ssh-key show`

Print the public key of the configured SSH deploy key

```bash
streamx project repo ssh-key show [options]
```

Add this public key to the Git hosting's deploy keys. The private key never leaves the platform.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../../global-options.md).
