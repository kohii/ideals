package org.rri.ideals.server.documentation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider;
import com.intellij.lang.documentation.impl.TargetsKt;
import com.intellij.openapi.editor.Editor;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IdealsDocumentationTargetProvider implements IdeDocumentationTargetProvider {

    @Override
    public @Nullable DocumentationTarget documentationTarget(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull LookupElement lookupElement
    ) {
        // Get the PSI element from the lookup element
        var psiElement = lookupElement.getPsiElement();
        if (psiElement == null) {
            return null;
        }
        // Get documentation targets for the PSI element's containing file at its offset
        var targets = TargetsKt.documentationTargets(psiElement.getContainingFile(), psiElement.getTextOffset());
        return targets.isEmpty() ? null : targets.get(0);
    }

    @Override
    public @NotNull List<? extends @NotNull DocumentationTarget> documentationTargets(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset
    ) {
        // Get documentation targets at the offset
        return TargetsKt.documentationTargets(file, offset);
    }
} 