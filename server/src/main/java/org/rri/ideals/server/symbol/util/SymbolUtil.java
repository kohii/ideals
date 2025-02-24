package org.rri.ideals.server.symbol.util;

import com.intellij.icons.AllIcons;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.DeferredIcon;
import com.intellij.ui.IconManager;
import com.intellij.ui.PlatformIcons;
import org.eclipse.lsp4j.SymbolKind;
import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.completions.util.IconUtil;

public class SymbolUtil {
  @NotNull
  public static SymbolKind getSymbolKind(@NotNull ItemPresentation presentation) {
    var parent = Disposer.newDisposable();
    try {
      // allow icon loading
      Registry.get("psi.deferIconLoading").setValue(false, parent);

      var icon = presentation.getIcon(false);

      SymbolKind kind = SymbolKind.Object;
      var iconManager = IconManager.getInstance();
      if (icon == null) {
        return SymbolKind.Object;
      }
      if (icon instanceof DeferredIcon deferredIcon) {
        icon = deferredIcon.getBaseIcon();
      }
      if (IconUtil.compareIcons(icon, AllIcons.Nodes.Method, PlatformIcons.Method) ||
          IconUtil.compareIcons(icon, AllIcons.Nodes.AbstractMethod, PlatformIcons.AbstractMethod)) {
        kind = SymbolKind.Method;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Module, "nodes/Module.svg")
          || IconUtil.compareIcons(icon, AllIcons.Nodes.IdeaModule, PlatformIcons.IdeaModule)
          || IconUtil.compareIcons(icon, AllIcons.Nodes.JavaModule, PlatformIcons.JavaModule)
          || IconUtil.compareIcons(icon, AllIcons.Nodes.ModuleGroup, "nodes/moduleGroup.svg")) {
        kind = SymbolKind.Module;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Function, PlatformIcons.Function)) {
        kind = SymbolKind.Function;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Interface, PlatformIcons.Interface) ||
          IconUtil.compareIcons(icon, iconManager.tooltipOnlyIfComposite(AllIcons.Nodes.Interface), PlatformIcons.Interface)) {
        kind = SymbolKind.Interface;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Type, "nodes/type.svg")) {
        kind = SymbolKind.TypeParameter;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Property, PlatformIcons.Property)) {
        kind = SymbolKind.Property;
      } else if (IconUtil.compareIcons(icon, AllIcons.FileTypes.Any_type, "fileTypes/any_type.svg")) {
        kind = SymbolKind.File;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Enum, PlatformIcons.Enum)) {
        kind = SymbolKind.Enum;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Variable, PlatformIcons.Variable) ||
          IconUtil.compareIcons(icon, AllIcons.Nodes.Parameter, PlatformIcons.Parameter) ||
          IconUtil.compareIcons(icon, AllIcons.Nodes.NewParameter, "nodes/newParameter.svg")) {
        kind = SymbolKind.Variable;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Constant, "nodes/constant.svg")) {
        kind = SymbolKind.Constant;
      } else if (
          IconUtil.compareIcons(icon, AllIcons.Nodes.Class, PlatformIcons.Class) ||
              IconUtil.compareIcons(icon,
                  iconManager.tooltipOnlyIfComposite(AllIcons.Nodes.Class), PlatformIcons.Class) ||
              IconUtil.compareIcons(icon, AllIcons.Nodes.Class, PlatformIcons.Class) ||
              IconUtil.compareIcons(icon, AllIcons.Nodes.AbstractClass, PlatformIcons.AbstractClass)) {
        kind = SymbolKind.Class;
      } else if (IconUtil.compareIcons(icon, AllIcons.Nodes.Field, PlatformIcons.Field)) {
        kind = SymbolKind.Field;
      }
      return kind;
    } finally {
      Disposer.dispose(parent);
    }
  }
}
