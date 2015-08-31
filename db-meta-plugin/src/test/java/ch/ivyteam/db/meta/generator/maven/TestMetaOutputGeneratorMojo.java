package ch.ivyteam.db.meta.generator.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.fest.assertions.api.FileAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.db.meta.generator.internal.HtmlDocGenerator;
import ch.ivyteam.db.meta.generator.internal.OracleSqlScriptGenerator;

public class TestMetaOutputGeneratorMojo
{
  private static final long RECENT_TIME_STAMP = new Date().getTime()-10000L;
  private static final long OLD_TIME_STAMP = 0L;
  
  @Rule
  public ProjectMojoRule<MetaOutputGeneratorMojo> mojoRule = new ProjectMojoRule<>(
          new File("src/test/resources/base"), MetaOutputGeneratorMojo.GOAL);
  private MetaOutputGeneratorMojo mojo;
  
  @Before
  public void before() throws IllegalAccessException
  {
    mojo = mojoRule.getMojo();
    mojoRule.setVariableValueToObject(mojo, "outputDirectory", new File(mojoRule.getProject().getBasedir(), "generated"));
    mojoRule.setVariableValueToObject(mojo, "inputDirectory", new File(mojoRule.getProject().getBasedir(), "meta"));
  }
  
  @Test
  public void executeWithOutputDir() throws IllegalAccessException, MojoExecutionException, MojoFailureException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", HtmlDocGenerator.class.getName());
    assertThat(getProjectFile("generated").listFiles()).isEmpty();
    mojo.execute();
    assertThatProjectFile("generated/IWA_ClusterHost.html").exists();
  }

  @Test
  public void directoryUpToDate() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", HtmlDocGenerator.class.getName());
    File file = getProjectFile("generated/blah.html");
    FileUtils.touch(file);
    mojo.execute();
    assertThatProjectFile("generated/IWA_ClusterHost.html").doesNotExist();
  }

  @Test
  public void directoryNotUpToDate() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", HtmlDocGenerator.class.getName());
    File file = getProjectFile("generated/blah.html");
    FileUtils.touch(file);
    file.setLastModified(OLD_TIME_STAMP);
    mojo.execute();
    assertThatProjectFile("generated/IWA_ClusterHost.html").exists();
  }

  @Test
  public void executeWithFile() throws IllegalAccessException, MojoExecutionException, MojoFailureException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    mojoRule.setVariableValueToObject(mojo, "outputFile", getProjectFile("generated/oracle.sql"));
    mojo.execute();
    assertThatProjectFile("generated/oracle.sql").exists();
  }

  @Test
  public void fileUpToDate() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    File sqlFile = getProjectFile("generated/oracle.sql");
    mojoRule.setVariableValueToObject(mojo, "outputFile", sqlFile);
        
    FileUtils.touch(sqlFile);
    sqlFile.setLastModified(RECENT_TIME_STAMP);
    
    mojo.execute();
    assertThat(getProjectFile("generated/oracle.sql").lastModified()).isEqualTo(RECENT_TIME_STAMP);
  }

  @Test
  public void fileNotUpToDate() throws IllegalAccessException, MojoExecutionException, MojoFailureException, IOException
  {
    mojoRule.setVariableValueToObject(mojo, "generatorClass", OracleSqlScriptGenerator.class.getName());
    File sqlFile = getProjectFile("generated/oracle.sql");
    mojoRule.setVariableValueToObject(mojo, "outputFile", sqlFile);
        
    FileUtils.touch(sqlFile);
    sqlFile.setLastModified(OLD_TIME_STAMP);
    
    mojo.execute();
    assertThat(getProjectFile("generated/oracle.sql").lastModified()).isGreaterThan(RECENT_TIME_STAMP);
  }

  private FileAssert assertThatProjectFile(String path)
  {
    return assertThat(getProjectFile(path));
  }

  private File getProjectFile(String path)
  {
    return new File(mojoRule.getProject().getBasedir(), path);
  }
}