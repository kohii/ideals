package org.rri.ideals.server.extensions;

import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.commands.ExecutorContext;
import org.rri.ideals.server.commands.LspCommand;

import java.util.function.Supplier;

public class ClassFileContentsCommand extends LspCommand<String> {
    @Override
    protected @NotNull Supplier<@NotNull String> getMessageSupplier() {
        return () -> "experimental/classFileContents call";
    }

    @Override
    protected boolean isCancellable() {
        return false;
    }

    @Override
    protected String execute(@NotNull ExecutorContext ctx) {
        return ctx.getEditor().getDocument().getText();
    }
}
