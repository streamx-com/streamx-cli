---
title: "streamx project create"
sidebar_label: "create"
sidebar_position: 2
description: "Create a project"
---

# `streamx project create`

Create a project

```bash
streamx project create [options] <name>
```

Optionally connects a Git repository and enables clusters in the same call. The endpoint is transactional: if any part fails, nothing is created.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<name>` | yes | Project name |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--cluster` | `<clusterId>` | Cluster to enable for the project (repeatable) |
| `-d`, `--description` | `<description>` | Project description |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-o`, `--output` | `<output>` | Specify output format: text, json, yaml |
| `--repository-branch` | `<branch>` | Git branch the platform deploys from |
| `--repository-uri` | `<uri>` | Git repository URI to connect to the project |
| `--ssh-private-key` | `<file>` | SSH private key file for private repositories (sent base64-encoded) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
