package org.rri.ideals.server.extensions;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Runnable(@NotNull String label, @NotNull Arguments args) {

    public record Arguments(
            // The working directory to run the command in.
            @NotNull String cwd,
            // Command to execute.
            @NotNull String cmd,
            // Arguments to pass to the executable.
            @NotNull List<String> executableArgs
    ) {
    }
}
