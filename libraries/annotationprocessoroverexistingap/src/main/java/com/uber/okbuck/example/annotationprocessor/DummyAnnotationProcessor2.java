package com.uber.okbuck.example.annotationprocessor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
public class DummyAnnotationProcessor2 extends AbstractProcessor {

  @Override
  public Set<String> getSupportedOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Generated.class.getName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element annotatedType: roundEnv.getElementsAnnotatedWith(Generated.class)) {
      try {
        generateDummyClass((TypeElement) annotatedType, processingEnv);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return true;
  }

  private static void generateDummyClass(TypeElement annotatedType, ProcessingEnvironment processingEnv)
      throws IOException {
    String generatedClassPackage = processingEnv.getElementUtils().getPackageOf(annotatedType).toString();
    String generatedClassName = annotatedType.getSimpleName().toString() + "_DummyGeneratedForGenerated";

    TypeSpec generatedTypeSpec = TypeSpec
        .classBuilder(generatedClassName)
        .addModifiers(PUBLIC)
        .build();

    JavaFile
        .builder(generatedClassPackage, generatedTypeSpec)
        .build()
        .writeTo(processingEnv.getFiler());
  }
}
