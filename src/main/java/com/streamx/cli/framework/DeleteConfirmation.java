package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import org.apache.commons.lang3.StringUtils;

public final class DeleteConfirmation {

  private DeleteConfirmation() {
  }

  public static void require(boolean force, String id) {
    if (force) {
      return;
    }
    String answer = InteractivePicker.pick(msg.deleteConfirmPrompt(id), null);
    if (StringUtils.isBlank(answer)) {
      throw new CliException(msg.deleteConfirmRequired());
    }
    if (!answer.strip().equals(id)) {
      throw new CliException(msg.deleteConfirmMismatch(id));
    }
  }
}
