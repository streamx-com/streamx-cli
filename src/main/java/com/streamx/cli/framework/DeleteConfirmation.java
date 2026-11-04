package com.streamx.cli.framework;

import static com.streamx.cli.i18n.MessageProvider.msg;

import org.apache.commons.lang3.StringUtils;

public final class DeleteConfirmation {

  private DeleteConfirmation() {
  }

  public static void require(boolean force, String id) {
    require(force, id, msg.deleteConfirmPrompt(id));
  }

  public static void require(boolean force, String id, String prompt) {
    if (force) {
      return;
    }
    String answer = InteractivePicker.pick(prompt, null);
    if (StringUtils.isBlank(answer)) {
      throw new CliException(msg.deleteConfirmRequired());
    }
    if (!answer.strip().equals(id)) {
      throw new CliException(msg.deleteConfirmMismatch(id));
    }
  }
}
