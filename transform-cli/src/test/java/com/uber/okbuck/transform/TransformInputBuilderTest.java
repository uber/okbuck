package com.uber.okbuck.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class TransformInputBuilderTest {

  private TransformInputBuilder builder;

  @Before
  public void setup() throws Exception {
    builder = new TransformInputBuilder();
  }

  @Test
  public void addJarInput_whenAddFile_builtShouldReturnFile() throws Exception {
    File file = getTmpFile("tmp");

    TransformInput input = builder.addJarInput(file).build();
    assertThat(input.getJarInputs()).hasSize(1);
    assertThat(input.getJarInputs().toArray(new JarInput[1])[0].getFile()).isEqualTo(file);
  }

  @Test
  public void addJarInput_whenAddPaths_builtShouldReturnCorrectPaths() throws Exception {
    File file1 = getTmpFile("tmp1");
    File file2 = getTmpFile("tmp2");

    TransformInput input =
        builder.addJarInput(file1.getAbsolutePath(), file2.getAbsolutePath()).build();
    assertThat(input.getJarInputs()).hasSize(2);

    JarInput[] jarInputs = input.getJarInputs().toArray(new JarInput[2]);
    assertThat(jarInputs[0].getFile().getAbsolutePath()).isEqualTo(file1.getAbsolutePath());
    assertThat(jarInputs[1].getFile().getAbsolutePath()).isEqualTo(file2.getAbsolutePath());
  }

  @Test
  public void addDirectoryInput_builtShouldReturnCorrectFile() throws Exception {
    File file = getTmpFile("tmp");

    TransformInput input = builder.addDirectoryInput(file).build();
    assertThat(input.getDirectoryInputs()).hasSize(1);
    assertThat(input.getDirectoryInputs().toArray(new DirectoryInput[1])[0].getFile())
        .isEqualTo(file);
  }

  private File getTmpFile(String prefix) throws IOException {
    File file = File.createTempFile(prefix, null);
    file.deleteOnExit();
    return file;
  }
}
