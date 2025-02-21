package org.rri.ideals.server;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.rri.ideals.server.extensions.Runnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@JsonSegment("experimental")
public interface ExperimentalProtocolExtensions {

    @JsonRequest
    CompletableFuture<String> classFileContents(TextDocumentIdentifier params);

    @JsonRequest
    CompletableFuture<List<Runnable>> runnables(TextDocumentIdentifier params);
}
