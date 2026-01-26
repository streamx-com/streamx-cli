# Contributing

- All commands should extend `AbstractCommand` from [this package](./streamx-cli/src/main/java/com/streamx/cli/framework).
- There are helper classes which extend the `AbstractCommand` class:
  - Use `AbstractCommandGroup` for commands which only contain subcommands and don't do anything else, e.g. `streamx settings`.
  - Use `AbstractSilentCommand` for commands which don't print any user-faced output, e.g. `streamx settings set`.
- Commands should throw only the [`CliException`](./streamx-cli/src/main/java/com/streamx/cli/framework/CliException.java).
- All user facing messages should be provided by [`MessageProvider`](./streamx-cli/src/main/java/com/streamx/cli/i18n/MessageProvider.java).

## Development

- Enter Quarkus development console.

`cd streamx-cli && ./mvnw quarkus:dev`

- Use `e` button to edit CLI arguments.

## Running tests

`cd streamx-cli && ./mvnw test`