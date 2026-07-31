package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

public final class DeleteConfirmation {

  private DeleteConfirmation() {
  }

  public static void require(boolean force, String id) {
    if (force) {
      return;
    }
    String answer = InteractivePicker.pick(msg.deleteConfirmPrompt(id), null);
    if (answer == null || answer.isBlank()) {
      throw new CliException(msg.deleteConfirmRequired());
    }
    if (!answer.strip().equals(id)) {
      throw new CliException(msg.deleteConfirmMismatch(id));
    }
  }
}
