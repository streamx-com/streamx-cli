---
title: "streamx auth login"
sidebar_label: "login"
sidebar_position: 1
description: "Log in to StreamX"
---

# `streamx auth login`

Log in to StreamX

```bash
streamx auth login [options]
```

Uses the browser (authorization code + PKCE) when a local browser is available,
and falls back to the device flow over SSH or with --no-browser.

## Options

| Option | Value | Description |
| --- | --- | --- |
| `--no-browser` |  | Use the device flow instead of opening a local browser (for SSH sessions and headless machines) |
| `-v`, `--verbose` |  | Print debug information |

---

Every command also accepts the [global options](../global-options.md).
