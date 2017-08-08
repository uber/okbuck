package com.uber.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.Collections;
import java.util.List;

public class SystemCurrentTimeMillisDetector extends Detector implements JavaPsiScanner {
    public static final String CHECK_METHOD_TO_EXCLUDE = "currentTimeMillis";
    public static final String CHECK_PACKAGE_TO_EXCLUDE = "java.lang.System";

    public static final String ISSUE_ID = "DontUseSystemTime";
    public static final Issue ISSUE = Issue.create(
        ISSUE_ID,
        "Don't use System.currentTimeMillis()",
        "This method is blocked from useage. This can't be easily mocked, injected, or tested.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        new Implementation(SystemCurrentTimeMillisDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(CHECK_METHOD_TO_EXCLUDE);
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor,
        PsiMethodCallExpression call, PsiMethod method) {
        String message = "System.currentTimeMillis() should not be used as this can't be easily mocked and tested.";
        context.report(ISSUE, call, context.getLocation(call), message);
    }
}
