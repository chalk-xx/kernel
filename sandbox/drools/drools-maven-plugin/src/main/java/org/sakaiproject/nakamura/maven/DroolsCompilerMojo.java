package org.sakaiproject.nakamura.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.RuleBase;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.agent.impl.KnowledgeAgentConfigurationImpl;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.compiler.PackageBuilderErrors;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.definition.rule.Rule;
import org.drools.definitions.impl.KnowledgePackageImp;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.io.ResourceFactory;
import org.drools.rule.Package;
import org.drools.runtime.StatelessKnowledgeSession;
import org.drools.util.BinaryRuleBaseLoader;
import org.drools.util.DroolsStreamUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @goal compile-rules
 */
public class DroolsCompilerMojo extends AbstractMojo {

  private static final String CHANGE_SET_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "   <change-set xmlns=\"http://drools.org/drools-5.0/change-set\" \n"
      + "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
      + "    xs:schemaLocation=\"http://drools.org/drools-5.0/change-set change-set.xsd\" > \n";
  private static final String CHANGE_SET_POSTFIX = "</change-set>";
  /**
   * @parameter default-value={"**\/*"}
   */
  private String[] includes;
  /**
   * @parameter default-value={}
   */
  private String[] excludes;
  /**
   * @parameter default-value="${basedir}/src/main/rules"
   */
  private File basedir;

  /**
   * The output directory for bundles.
   * 
   * @parameter default-value="${project.build.directory}/classes"
   */
  private File outputDirectory;
  /**
   * The output directory for bundles.
   * 
   * @parameter default-value=
   *            "SLING-INF/content/var/rules/${project.groupId}/${project.artifactId}/${project.version}/${project.artifactId}-${project.version}.pkg"
   */
  private String packageOutputName;

  public void execute() throws MojoExecutionException {
    // find all the rules items and load them into a package
    try {

      URLClassLoader uc = new URLClassLoader(new URL[] { outputDirectory.toURL() }, this
          .getClass().getClassLoader()) {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          Class<?> c= super.findClass(name);
          getLog().info("Fincing Class for compile ["+name+"] found ["+c+"]");
          return c;
        }
      };
      URLClassLoader uc2 = new URLClassLoader(new URL[] { outputDirectory.toURL() }, this
          .getClass().getClassLoader()) {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
          Class<?> c= super.findClass(name);
          getLog().info("Finding Class for runtime ["+name+"] found ["+c+"]");
          return c;
        }
      };
      getLog().info("Package Class loader is using classpath " + Arrays.toString(uc.getURLs()));

      PackageBuilderConfiguration packageBuilderConfiguration = new PackageBuilderConfiguration(
          uc);
      PackageBuilder pb = new PackageBuilder(packageBuilderConfiguration);

      DirectoryScanner ds = new DirectoryScanner();
      ds.setIncludes(includes);
      ds.setExcludes(excludes);
      ds.setBasedir(basedir);
      ds.setCaseSensitive(true);
      ds.scan();

      String[] files = ds.getIncludedFiles();
      for (String file : files) {
        File f = new File(basedir, file);
        Reader reader = new FileReader(f);
        try {
          if (file.endsWith(".drl")) {
            getLog().info("Adding Rules " + f);
            pb.addPackageFromDrl(reader);
          } else if (file.endsWith(".xml")) {
            getLog().info("Adding Package definition " + f);
            pb.addPackageFromXml(reader);
          } else if (file.endsWith(".rf")) {
            getLog().info("Adding Rule Flow " + f);
            pb.addRuleFlow(reader);
          } else {
            getLog().info("Ignored Resource " + f);
          }

        } finally {
          reader.close();
        }
      }

      pb.compileAll();
      PackageBuilderErrors errors = pb.getErrors();
      if (errors.size() > 0) {
        for (KnowledgeBuilderError kberr : errors) {
          getLog().error(kberr.toString());
        }
        throw new MojoExecutionException("Package is not valid");

      }
      org.drools.rule.Package p = pb.getPackage();
      if (!p.isValid()) {
        getLog().error("Package is not valid ");
        throw new MojoExecutionException("Package is not valid");
      }

      File outputFile = getOutputFile();
      getLog().info("Saving compiled package to  "+outputFile.getPath());
      outputFile.getParentFile().mkdirs();
      FileOutputStream fout = new FileOutputStream(outputFile);
      DroolsStreamUtils.streamOut(fout, p);
      fout.close();

      
      
      getLog().info("Testing Compiled package "+outputFile.getPath());

      File inputFile = getOutputFile();
      FileInputStream fin = new FileInputStream(inputFile);
      
      RuleBaseConfiguration config = new RuleBaseConfiguration(uc2);
      RuleBase ruleBase = RuleBaseFactory.newRuleBase(config);
      Object o = DroolsStreamUtils.streamIn(fin, uc);

     
      ruleBase.addPackage((Package) o);
      KnowledgeBase kb = new KnowledgeBaseImpl(ruleBase);

      StatelessKnowledgeSession session = kb.newStatelessKnowledgeSession();
      
      getLog().info("Testing passed ");


    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(e.getMessage());
    }

  }

  private File getOutputFile() {
    return new File(outputDirectory, packageOutputName);
  }
}
