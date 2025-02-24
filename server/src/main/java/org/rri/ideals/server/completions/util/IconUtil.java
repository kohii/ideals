package org.rri.ideals.server.completions.util;

import com.intellij.openapi.util.DummyIcon;
import com.intellij.ui.PlatformIcons;
import com.intellij.ui.icons.CompositeIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class IconUtil {
  static public boolean compareIcons(@NotNull Icon elementIcon, @NotNull Icon standardIcon, @NotNull PlatformIcons platformIcon) {
    return compareIcons(elementIcon, standardIcon, platformIcon.toString());
  }

  static public boolean compareIcons(@NotNull Icon elementIcon, @NotNull Icon standardIcon, @NotNull String iconPath) {
    // in all cases the first icon in CompositeIcons is actually the main icon
    while (elementIcon instanceof CompositeIcon compositeIcon) {
      if (compositeIcon.getIconCount() == 0) {
        break;
      }

      elementIcon = compositeIcon.getIcon(0);
    }

    return elementIcon != null &&
        (elementIcon.equals(standardIcon) ||
            ((elementIcon instanceof DummyIcon d) && iconPath.equals(d.getOriginalPath())));
  }
}
