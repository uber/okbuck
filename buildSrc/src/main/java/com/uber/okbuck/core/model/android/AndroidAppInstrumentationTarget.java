package com.uber.okbuck.core.model.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.base.Preconditions;
import java.util.Set;
import org.gradle.api.Project;

/** An Android instrumentation target */
public class AndroidAppInstrumentationTarget extends AndroidInstrumentationTarget {

  AndroidAppInstrumentationTarget(Project project, String name) {
    super(project, name);
  }

  @Override
  protected BaseVariant getBaseVariant() {
    AppExtension androidExtension = (AppExtension) getAndroidExtension();

    Set<ApplicationVariant> applicationVariants =
        androidExtension
            .getApplicationVariants()
            .matching(
                applicationVariant ->
                    applicationVariant.getName().equals(getMainTargetName(getName())));
    Preconditions.checkArgument(applicationVariants.size() > 0);

    return applicationVariants.iterator().next().getTestVariant();
  }
}
