package org.rri.ideals.server.symbol;

import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereToggleAction;
import com.intellij.ide.actions.searcheverywhere.SymbolSearchEverywhereContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.StandardProgressIndicator;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rri.ideals.server.LspPath;
import org.rri.ideals.server.symbol.util.SymbolUtil;
import org.rri.ideals.server.util.MiscUtil;
import javax.swing.Icon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
final public class WorkspaceSymbolService {
  @NotNull
  private final Project project;

  private final int LIMIT = 100;

  private static final Comparator<WorkspaceSearchResult> COMP = Comparator.comparingInt(WorkspaceSearchResult::weight).reversed();

  public WorkspaceSymbolService(@NotNull Project project) {
    this.project = project;
  }

  @SuppressWarnings("deprecation")
  public @NotNull CompletableFuture<@NotNull Either<List<? extends SymbolInformation>, @Nullable List<? extends WorkspaceSymbol>>> runSearch(String pattern) {
    return CompletableFutures.computeAsync(AppExecutorUtil.getAppExecutorService(),
        cancelToken -> {
          if (DumbService.isDumb(project)) {
            return Either.forRight(null);
          }
          final var result = execute(pattern, cancelToken).stream()
              .map(WorkspaceSearchResult::symbol)
              .toList();
          return Either.forRight(result);
        });
  }

  private @NotNull List<@NotNull WorkspaceSearchResult> execute(@NotNull String pattern, @Nullable CancelChecker cancelToken) {
    Ref<SymbolSearchEverywhereContributor> contributorRef = new Ref<>();
    ApplicationManager.getApplication().invokeAndWait(
        () -> {
          final var context = SimpleDataContext.getProjectContext(project);
          final var event = AnActionEvent.createEvent(context, null, "keyboard shortcut", ActionUiKind.NONE, null);
          final var contributor = new SymbolSearchEverywhereContributor(event);
          if (!pattern.isEmpty()) {
            final var actions = contributor.getActions(() -> {
            });
            final var everywhereAction = (SearchEverywhereToggleAction) ContainerUtil.find(actions, o -> o instanceof SearchEverywhereToggleAction);
            everywhereAction.setEverywhere(true);
          }
          contributorRef.set(contributor);
        }
    );
    final var ref = new Ref<List<WorkspaceSearchResult>>();
    ApplicationManager.getApplication().runReadAction(
        () -> ref.set(search(contributorRef.get(), pattern.isEmpty() ? "*" : pattern, cancelToken)));
    return ref.get();
  }

  private record WorkspaceSearchResult(@NotNull WorkspaceSymbol symbol,
                                       @NotNull PsiElement element,
                                       int weight,
                                       boolean isProjectFile) {
  }

  // Returns: the list of founded symbols
  // Note: Project symbols first then symbols from libraries, jdks, environments...
  public @NotNull List<@NotNull WorkspaceSearchResult> search(
      @NotNull SymbolSearchEverywhereContributor contributor,
      @NotNull String pattern,
      @Nullable CancelChecker cancelToken) {
    final var allSymbols = new ArrayList<WorkspaceSearchResult>(LIMIT);
    final var scope = ProjectScope.getProjectScope(project);
    final var elements = new HashSet<PsiElement>();
    final var processedFiles = new HashSet<PsiFile>();
    try {
      final var indicator = new WorkspaceSymbolIndicator(cancelToken);
      ApplicationManager.getApplication().executeOnPooledThread(() ->
          contributor.fetchWeightedElements(pattern, indicator,
              descriptor -> {
                if (!(descriptor.getItem() instanceof final PsiElement elem)
                    || elements.contains(elem)) {
                  return true;
                }
                final var searchResult = toSearchResult(descriptor, scope);
                if (searchResult == null) {
                  return true;
                }
                elements.add(elem);
                allSymbols.add(searchResult);

                // Add Kotlin file symbol if we haven't processed this file yet
                final var psiFile = elem.getContainingFile();
                if (psiFile != null && !processedFiles.contains(psiFile)) {
                  processedFiles.add(psiFile);
                  final var virtualFile = psiFile.getVirtualFile();
                  if (virtualFile != null && virtualFile.getName().endsWith(".kt") && virtualFile.getName().equals("DocumentSymbol.kt")) {
                    final var ktFileName = virtualFile.getNameWithoutExtension() + "Kt";
                    if (pattern.equals("*") || ktFileName.toLowerCase().contains(pattern.toLowerCase())) {
                      final var ktFileSymbol = new WorkspaceSymbol(
                          ktFileName,
                          SymbolKind.Object,
                          Either.forLeft(new Location(
                              LspPath.fromVirtualFile(virtualFile).toLspUri(),
                              new Range(new Position(0, 0), new Position(psiFile.getText().split("\n").length, 0))
                          )),
                          null);
                      allSymbols.add(new WorkspaceSearchResult(ktFileSymbol, psiFile, 0, scope.contains(virtualFile)));
                    }
                  }
                }
                return allSymbols.size() < LIMIT;
              })).get();
    } catch (InterruptedException | ExecutionException ignored) {
    }
    // Sort by weight first, then by isProjectFile (project files first), then by name
    allSymbols.sort(
        COMP.thenComparing((a, b) -> Boolean.compare(b.isProjectFile(), a.isProjectFile()))
            .thenComparing(a -> a.symbol().getName())
    );
    return allSymbols;
  }

  private static @Nullable WorkspaceSearchResult toSearchResult(@NotNull FoundItemDescriptor<@NotNull Object> descriptor,
                                                                @NotNull SearchScope scope) {
    if (!(descriptor.getItem() instanceof final PsiElement elem)) {
      return null;
    }
    if (!(elem instanceof NavigationItem navigationItem)) {
      return null;
    }
    var itemPresentation = navigationItem.getPresentation();
    if (itemPresentation == null) {
      return null;
    }
    final var psiFile = elem.getContainingFile();
    if (psiFile == null) {
      return null;
    }
    final var virtualFile = psiFile.getVirtualFile();
    String containerName = null;
    if (elem.getParent() instanceof PsiNameIdentifierOwner parent) {
      containerName = parent.getName();
    } else if (elem.getParent() != null && elem.getParent().getParent() instanceof PsiNameIdentifierOwner grandParent) {
      containerName = grandParent.getName();
    }
    final var location = new Location();
    SymbolKind kind = SymbolUtil.getSymbolKind(itemPresentation);
    if (elem instanceof PsiFile) {
      kind = SymbolKind.File;
    }
    location.setUri(LspPath.fromVirtualFile(virtualFile).toLspUri());
    final var symbol = new WorkspaceSymbol(
        itemPresentation.getPresentableText(),
        kind,
        Either.forLeft(MiscUtil.psiElementToLocation(elem, psiFile)),
        containerName);
    return new WorkspaceSearchResult(symbol, elem, descriptor.getWeight(), scope.contains(virtualFile));
  }

  private static class WorkspaceSymbolIndicator extends AbstractProgressIndicatorBase implements StandardProgressIndicator {
    @Nullable
    private final CancelChecker cancelToken;

    public WorkspaceSymbolIndicator(@Nullable CancelChecker cancelToken) {
      this.cancelToken = cancelToken;
    }

    @Override
    public void checkCanceled() {
      if (cancelToken != null) {
        try {
          cancelToken.checkCanceled();
        } catch (CancellationException e) {
          throw new ProcessCanceledException(e);
        }
      }
      super.checkCanceled();
    }
  }
}
