---
title: "streamx project repo ssh-key set"
sidebar_label: "set"
sidebar_position: 3
description: "Set the SSH private key used to access the repository"
---

# `streamx project repo ssh-key set`

Set the SSH private key used to access the repository

```bash
streamx project repo ssh-key set [options] <file>
```

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<file>` | yes | SSH private key file (sent base64-encoded) |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `--project` | `<projectId>` | Project ID (defaults to the current project) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../../global-options.md).
