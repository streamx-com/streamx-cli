package com.streamx.cli;

import com.streamx.cli.commands.publish.EventTemplatePlaceholders;
import com.streamx.cli.commands.publish.event.EventCommandResult;
import com.streamx.cli.commands.publish.event.EventTemplateCatalog;
import com.streamx.cli.commands.publish.events.EventsCommandResult;
import com.streamx.cli.commands.publish.stream.StreamCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.copy.CopyCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.create.CreateCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.delete.DeleteCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.edit.EditCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.list.ListCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.rename.RenameCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.resetdefaulttemplates.ResetDefaultTemplatesCommandResult;
import com.streamx.cli.commands.settings.eventtemplates.validate.ValidateCommandResult;
import com.streamx.runner.StreamxRunner;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;

@RegisterForReflection(targets = {StreamxRunner.class, SimpleLog.class, LogFactoryImpl.class,
    Main.class, EventTemplatePlaceholders.class, EventTemplateCatalog.class,
    EventTemplateCatalog.TemplateLocation[].class, RenameCommandResult.class,
    ResetDefaultTemplatesCommandResult.class, EventCommandResult.class, EventsCommandResult.class,
    StreamCommandResult.class, CopyCommandResult.class, CreateCommandResult.class,
    DeleteCommandResult.class, EditCommandResult.class, ListCommandResult.class,
    ValidateCommandResult.class}, registerFullHierarchy = true)
public class ReflectionConfiguration {
}
