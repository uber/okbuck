package com.uber.okbuck.core.util;

import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.TargetCache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.gradle.api.Project;

public class ProjectCache {

  private static final String SCOPE_CACHE = "okbuckScopeCache";
  private static final String TARGET_CACHE = "okbuckTargetCache";

  private ProjectCache() {}

  public static Map<String, Scope> getScopeCache(Project project) {
    String scopeCacheKey = getCacheKey(project, SCOPE_CACHE);

    Map<String, Scope> scopeCache = (Map<String, Scope>) project.property(scopeCacheKey);
    if (scopeCache == null) {
      throw new RuntimeException(
          "Scope cache external property '" + scopeCacheKey + "' is not set.");
    }
    return scopeCache;
  }

  public static TargetCache getTargetCache(Project project) {
    String targetCacheKey = getCacheKey(project, TARGET_CACHE);

    TargetCache targetCache = (TargetCache) project.property(targetCacheKey);
    if (targetCache == null) {
      throw new RuntimeException(
          "Target cache external property '" + targetCacheKey + "' is not set.");
    }
    return targetCache;
  }

  public static void initScopeCache(Project project) {
    project
        .getExtensions()
        .getExtraProperties()
        .set(getCacheKey(project, SCOPE_CACHE), new ConcurrentHashMap<>());
  }

  public static void resetScopeCache(Project project) {
    project.getExtensions().getExtraProperties().set(getCacheKey(project, SCOPE_CACHE), null);
  }

  public static void initTargetCacheForAll(Project project) {
    initTargetCache(project);
    project.getSubprojects().forEach(ProjectCache::initTargetCache);
  }

  public static void resetTargetCacheForAll(Project project) {
    resetTargetCache(project);
    project.getSubprojects().forEach(ProjectCache::resetTargetCache);
  }

  private static void initTargetCache(Project project) {
    project
        .getExtensions()
        .getExtraProperties()
        .set(getCacheKey(project, TARGET_CACHE), new TargetCache(project));
  }

  private static void resetTargetCache(Project project) {
    project.getExtensions().getExtraProperties().set(getCacheKey(project, TARGET_CACHE), null);
  }

  private static String getCacheKey(Project project, String prefix) {
    return prefix + project.getPath();
  }
}
