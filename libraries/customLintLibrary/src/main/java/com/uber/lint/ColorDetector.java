package com.uber.lint;

import com.android.SdkConstants;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import java.util.Collection;
import org.w3c.dom.Attr;

public class ColorDetector extends ResourceXmlDetector {

  @SuppressWarnings("unchecked")
  private static final Implementation IMPLEMENTATION =
      new Implementation(
          ColorDetector.class,
          Scope.MANIFEST_AND_RESOURCE_SCOPE,
          Scope.MANIFEST_SCOPE,
          Scope.RESOURCE_FILE_SCOPE);

  public static final Issue ISSUE =
      Issue.create(
          "AndroidColorDetector",
          "Using Android color resources",
          "Android color resources like android:color/white should not be used."
              + " Manufacturers tend to overwrite them.",
          Category.CORRECTNESS,
          7,
          Severity.ERROR,
          IMPLEMENTATION);

  public ColorDetector() {}

  @Override
  public Collection<String> getApplicableAttributes() {
    return ALL;
  }

  @Override
  public void visitAttribute(XmlContext context, Attr attribute) {
    if (attribute.getValue().contains(SdkConstants.ANDROID_COLOR_RESOURCE_PREFIX)) {
      context.report(
          ISSUE,
          attribute,
          context.getLocation(attribute),
          "Using Android color resources are not recommended. Manufacturers are overriding them with other colors.");
    }
  }
}
