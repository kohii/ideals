package org.rri.ideals.server;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public abstract class LspLightBasePlatformTestCase extends BasePlatformTestCase {

  @Override
  protected boolean isIconRequired() {
    return true;
  }

  @Override
  protected void tearDown() throws Exception {
    for (var openEditor : EditorFactory.getInstance().getAllEditors()) {
      EditorFactory.getInstance().releaseEditor(openEditor);
    }
    super.tearDown();
  }
}
