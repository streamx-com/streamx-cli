# Contributing

- All commands should extend `AbstractCommand` from [this package](./src/main/java/com/streamx/cli/framework).
- There are helper classes which extend the `AbstractCommand` class:
  - Use `AbstractCommandGroup` for commands which only contain subcommands and don't do anything else, e.g. `streamx settings`.
  - Use `AbstractSilentCommand` for commands which don't print any user-faced output, e.g. `streamx settings set`.
- Commands should throw only the [`CliException`](./src/main/java/com/streamx/cli/framework/CliException.java).
- All user facing messages should be provided by [`MessageProvider`](./src/main/java/com/streamx/cli/i18n/MessageProvider.java).

## Build

- Ensure that you have Java 25 GraalVM installed.
- First you need to run `./mvnw clean verify` to build the project with.
  It's needed to generate metadata for building native-image in the next step.

- Then you can run `./mvnw verify -Dnative` **without the clean goal** to build the native executable.

## Development

**IMPORTANT:** mark test which run StreamX mesh with the `DisabledIfDockerUnavaliable` annotation.
Otherwise, CI will fail because at this moment Docker isn't supported on macOS arm64.

- Enter Quarkus development console.

`./mvnw quarkus:dev`

- Use `e` button to edit CLI arguments.

## Running tests

`./mvnw verify -Dnative`

## Release process

Use `./release.sh <patch|minor|major>` script.