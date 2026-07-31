---
title: "streamx project repo ssh-key generate"
sidebar_label: "generate"
sidebar_position: 1
description: "Generate a new SSH key pair server-side and save it to files"
---

# `streamx project repo ssh-key generate`

Generate a new SSH key pair server-side and save it to files

```bash
streamx project repo ssh-key generate [options] <file>
```

The private key is written to &lt;file&gt; (mode 600) and the public key to &lt;file&gt;.pub; the keys are not printed and not stored on the platform. Pass the private key to 'ssh-key set' or 'project create --ssh-private-key'; add the public key to the Git hosting's deploy keys.

## Arguments

| Argument | Required | Description |
| --- | --- | --- |
| `<file>` | yes | Where to save the private key; the public key goes to &lt;file&gt;.pub |

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--org` | `<orgId>` | Organization ID (defaults to the current organization) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../../../global-options.md).
