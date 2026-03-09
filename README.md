# StreamX CLI v2.x

[StreamX](https://streamx.dev) is a globally distributed experience delivery that outperforms traditional CDNs.

![logo](./logo.svg)

This project provides utilities for managing the mesh:
* It allows you to run a defined mesh from commands.
* It allows you to ingest data into a mesh.

For more information, see the [StreamX CLI Reference](https://www.streamx.dev/guides/main/streamx-command-line-interface-reference.html).

Please read the [contributing guidelines](./CONTRIBUTING.md) if you're a developer and wish to contribute to the project.

## Installation

### Install from releases page (macOS, Linux)

You can find all the available stable releases on [GitHub releases page](https://github.com/streamx-com/streamx-cli/releases).

### Using Homebrew (macOS, Linux)

**Install latest stable version:**

```sh
brew install streamx-com/tap/streamx 
```

**Install specific stable version:**

```sh
# Add brew tap
brew tap streamx-com/tap

# List available formulas
brew search streamx-com/tap

# Install the specific versioned formula from the previous step
brew install <formula>
```

**Install preview version:**

Use the commands above as for stable releases, but replace `streamx-com/tap` with `streamx-com/preview-tap`.

## Configuration

There are several ways of configuring and several properties to configure.

For details refer to [StreamX CLI Reference](https://www.streamx.dev/guides/streamx-command-line-interface-reference.html).
