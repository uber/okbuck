package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class BazelAndroidLibraryRule extends BuckRule {
    private static final RuleType RULE_TYPE = RuleType.ANDROID_LIBRARY

    private final Set<String> srcSet
    private final String manifest
    private final Set<String> providedDeps
    private final String packageName
    private final Set<AndroidTarget.ResBundle> resources

    BazelAndroidLibraryRule(
            String name,
            String packageName,
            List<String> deps,
            Set<String> srcSet,
            String manifest,
            Set<String> providedDeps,
            Set<AndroidTarget.ResBundle> resources) {
        super(RULE_TYPE, name, [], deps)
        this.srcSet = srcSet
        this.manifest = manifest
        this.providedDeps = providedDeps
        this.packageName = packageName
        this.resources = resources
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tcustom_package = '${packageName}',")
        printer.println("\tsrcs = glob([")
        for (String src : srcSet) {
            printer.println("\t\t'${src}/**/*.java',")
        }
        printer.println("\t]),")

        printer.println("\tresource_files = glob([")
        for (AndroidTarget.ResBundle bundle : resources) {
            printer.println("\t\t'${bundle.resDir}/**',")
        }
        printer.println("\t]),")
        if (!resources.isEmpty()) {
            def assetsDir = resources.first().assetsDir
            if (assetsDir != null) {
                printer.println("\tassets_dir = '${resources.first().assetsDir}',")
                printer.println("\tassets = glob(['${resources.first().assetsDir}/**']),")
            }
        }

        if (manifest != null && !manifest.isEmpty()) {
            printer.println("\tmanifest = '${manifest}',")
        }

        if (!providedDeps.empty) {
            printer.println("\texports = [")
            for (String dep : providedDeps.sort()) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }
    }

}
