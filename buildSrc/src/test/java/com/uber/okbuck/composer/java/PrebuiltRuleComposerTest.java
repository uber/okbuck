package com.uber.okbuck.composer.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.uber.okbuck.core.dependency.OExternalDependency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class PrebuiltRuleComposerTest {

  @Test
  public void getLabels_withValidLabelsMap_returnsLabels() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);
    Map<String, List<String>> labelsMap = new HashMap<>();
    List<String> expectedLabels = new ArrayList<>();
    expectedLabels.add("test_label=value1,value2");
    labelsMap.put("com.example:test-artifact:1.0.0", expectedLabels);

    when(dependency.getMavenCoordsForValidation()).thenReturn("com.example:test-artifact:1.0.0");

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, labelsMap);

    // Assert
    assertNotNull(result);
    assertEquals(expectedLabels.size(), result.size());
    assertTrue(result.contains("test_label=value1,value2"));
  }

  @Test
  public void getLabels_withNullLabelsMap_returnsEmptySet() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, null);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getLabels_withEmptyLabelsMap_returnsEmptySet() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);
    Map<String, List<String>> emptyLabelsMap = new HashMap<>();

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, emptyLabelsMap);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getLabels_withMissingKey_returnsEmptySet() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);
    Map<String, List<String>> labelsMap = new HashMap<>();
    List<String> labels = new ArrayList<>();
    labels.add("other_label=other_value");
    labelsMap.put("com.other:other-artifact:2.0.0", labels);

    when(dependency.getMavenCoordsForValidation()).thenReturn("com.example:test-artifact:1.0.0");

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, labelsMap);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getLabels_withNullValue_returnsEmptySet() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);
    Map<String, List<String>> labelsMap = new HashMap<>();
    labelsMap.put("com.example:test-artifact:1.0.0", null);

    when(dependency.getMavenCoordsForValidation()).thenReturn("com.example:test-artifact:1.0.0");

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, labelsMap);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getLabels_withEmptyValue_returnsEmptySet() {
    // Arrange
    OExternalDependency dependency = mock(OExternalDependency.class);
    Map<String, List<String>> labelsMap = new HashMap<>();
    labelsMap.put("com.example:test-artifact:1.0.0", new ArrayList<>());

    when(dependency.getMavenCoordsForValidation()).thenReturn("com.example:test-artifact:1.0.0");

    // Act
    Set<String> result = PrebuiltRuleComposer.getLabels(dependency, labelsMap);

    // Assert
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
