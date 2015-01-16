package ch.ivyteam.maven.public_api_source;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.source.AbstractSourceJarMojo;
import org.eclipse.tycho.source.OsgiSourceMojo;

/**
 * 
 * @extendsPlugin tycho-source
 * @extendsGoal plugin-source
 * @goal extended-plugin-source
 * @phase prepare-package
 */
public class PublicApiSourceMojo extends OsgiSourceMojo
{
  static final String GOAL = "extended-plugin-source";

  /**
   * Whether the sources of ivy Public API should be added.
   * 
   * @parameter default-value="true"
   */
  boolean includePublicApiSource;
  
  @Override
  public void execute() throws MojoExecutionException
  {
    getLog().debug("executing goal " + GOAL);
    avoidIncludeAnythingIfNotConfigured();
    super.execute();
  }
  
  /**
   * To prevent including everything when nothing is configured.
   */
  private void avoidIncludeAnythingIfNotConfigured()
  {
    try
    {
      Field field = AbstractSourceJarMojo.class.getDeclaredField("includes");
      try
      {
        field.setAccessible(true);
        String[] value = (String[])field.get(this);
        if (value == null || value.length == 0)
        {
          field.set(this, new String[] {"."});
        }
      }
      finally
      {
        field.setAccessible(false);
      }
    }
    catch (Exception ex)
    {
      getLog().error(ex);
    }
    
  }

  @Override
  protected List<Resource> getResources(MavenProject p) throws MojoExecutionException
  {
    List<Resource> resources = new ArrayList<Resource>();
    resources.addAll(super.getResources(p));
    if (includePublicApiSource)
    {
      PublicApiClassesFinder finder = new PublicApiClassesFinder(new File( getClassesDir(p)));
      List<File> foundPublicApiClasses = finder.find();

      List<String> allSourceFolders = getAllSourceFolders(p);
      for (String sourceFolder : allSourceFolders) 
      {
        List<String> publicApiFiles = buildPublicApiFilesForSourceFolder(p, foundPublicApiClasses, sourceFolder);
        if (!publicApiFiles.isEmpty())
        {
          resources.add(
                  createResource(
                          new File(project.getBasedir(), sourceFolder).getAbsolutePath(),
                          publicApiFiles,
                          null));
        }
      }
      for (File notAssignedPublicApiClass : foundPublicApiClasses)
      {
        getLog().warn("No source File found for " + notAssignedPublicApiClass.getAbsolutePath());
      }
    }
    return resources;
  }

  private List<String> buildPublicApiFilesForSourceFolder(MavenProject p, List<File> foundPublicApiClasses,
          String sourceFolder)
  {
    List<String> publicApiFiles = new ArrayList<>();
    Iterator<File> foundClassesIterator = foundPublicApiClasses.iterator();
    while (foundClassesIterator.hasNext())
    {
      File foundClass = foundClassesIterator.next();
      String correspondingSourcePath = convertClassPathToSourcePath( getClassesDir(p), foundClass);
      File correspondingSourceFile = new File(new File(p.getBasedir(), sourceFolder), correspondingSourcePath);
      if (correspondingSourceFile.exists())
      {
        publicApiFiles.add(correspondingSourcePath);
        foundClassesIterator.remove();
      }
    }
    return publicApiFiles;
  }
  
  private List<String> getAllSourceFolders(MavenProject p)
  {
    BuildProperties buildProps = getBuildPropertiesParser().parse(p.getBasedir());
    List<String> sourceFolders = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : buildProps.getJarToSourceFolderMap().entrySet()) {
      for (String sourceFolder : entry.getValue()) {
        sourceFolders.add(sourceFolder);
      }
    }
    return sourceFolders;
  }

  private String getClassesDir(MavenProject p)
  {
    return p.getBuild().getOutputDirectory();
  }

  private String convertClassPathToSourcePath(String classesDir, File foundClass)
  {
    String absolutePath = foundClass.getAbsolutePath();
    String relativePath = StringUtils.removeStart(absolutePath, classesDir + File.separator);
    String correspondingSourcePath = FilenameUtils.removeExtension(relativePath) + ".java";
    return correspondingSourcePath;
  }

  private static Resource createResource(String directory, List<String> includes, List<String> excludes)
  {
    Resource resource = new Resource();
    resource.setDirectory(directory);
    resource.setExcludes(excludes);
    resource.setIncludes(includes);
    return resource;
  }

  public MavenProject getProject()
  {
    return project;
  }
  
  void setProject(MavenProject project)
  {
    this.project = project;
  }

  /**
   * Overridden since we have another plugin key (<i>ch.ivyteam:public-api-source-maven-plugin</i>)
   * compared to the plugin we extend.
   */
  @Override
  protected boolean isRelevantProject(MavenProject mvnProject)
  {
    BuildPropertiesParser buildPropertiesParser = getBuildPropertiesParser();
    if (buildPropertiesParser != null)
    {
      return isRelevantProjectImpl(mvnProject, buildPropertiesParser);
    }
    return false;
  }

  private BuildPropertiesParser getBuildPropertiesParser()
  {
    try
    {
      Field field = OsgiSourceMojo.class.getDeclaredField("buildPropertiesParser");
      try
      {
        field.setAccessible(true);
        BuildPropertiesParser buildPropertiesParser = (BuildPropertiesParser) field.get(this);

        return buildPropertiesParser;
      }
      finally
      {
        field.setAccessible(false);
      }
    }
    catch (Exception ex)
    {
      getLog().error(ex);
    }

    return null;
  }

  protected static boolean isRelevantProjectImpl(MavenProject project,
          BuildPropertiesParser buildPropertiesParser)
  {
    String packaging = project.getPackaging();
    boolean relevant = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging)
            || org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
    if (!relevant)
    {
      return false;
    }

    // this assumes that sources generation has to be explicitly enabled in
    // pom.xml
    Plugin plugin = project.getPlugin("ch.ivyteam:public-api-source-maven-plugin");

    if (plugin == null)
    {
      return false;
    }

    for (PluginExecution execution : plugin.getExecutions())
    {
      if (execution.getGoals().contains(GOAL))
      {
        boolean requireSourceRoots = Boolean.parseBoolean(getParameterValue(execution, "requireSourceRoots",
                "false"));
        if (requireSourceRoots)
        {
          return true;
        }
        boolean hasAdditionalFilesets = getConfigurationElement((Xpp3Dom) execution.getConfiguration(),
                "additionalFileSets") != null;
        if (hasAdditionalFilesets)
        {
          return true;
        }
        BuildProperties buildProperties = buildPropertiesParser.parse(project.getBasedir());
        if (buildProperties.getJarToSourceFolderMap().size() > 0
                || buildProperties.getSourceIncludes().size() > 0)
        {
          return true;
        }
      }
    }

    return false;
  }

  private static String getParameterValue(PluginExecution execution, String name, String defaultValue)
  {
    String value = getElementValue((Xpp3Dom) execution.getConfiguration(), name);
    return value != null ? value : defaultValue;
  }

  private static String getElementValue(Xpp3Dom config, String name)
  {
    Xpp3Dom child = getConfigurationElement(config, name);
    if (child == null)
    {
      return null;
    }
    return child.getValue();
  }

  private static Xpp3Dom getConfigurationElement(Xpp3Dom config, String name)
  {
    if (config == null)
    {
      return null;
    }
    Xpp3Dom child = config.getChild(name);
    return child;
  }

}