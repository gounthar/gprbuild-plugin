/*
 * The MIT License
 *
 * Copyright 2018 Felix Patschkowski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.gpr;

/**
 * Build using GPRbuild.
 *
 * @author Felix Patschkowski
 */
public class GprbuildBuilder extends hudson.tasks.Builder implements
        jenkins.tasks.SimpleBuildStep {    
    
    @org.jenkinsci.Symbol("gprbuild")
    @hudson.Extension
    public static final class DescriptorImpl extends
            hudson.tasks.BuildStepDescriptor<hudson.tasks.Builder> {
        
        public hudson.util.ListBoxModel doFillInstallationNameItems(
                @org.kohsuke.stapler.QueryParameter String installationName)
        {
            hudson.util.ListBoxModel items = new hudson.util.ListBoxModel();

            for (GnatInstallation installation :
                    GnatInstallation.allInstallations()) {
                items.add(new hudson.util.ListBoxModel.Option(
                        installation.getName(),
                        installation.getName(),
                        installation.getName().equals(installationName)));
            }

            return items;
        }
        
        @Override
        public String getDisplayName()
        {
            return Messages.GprbuildBuilder_DisplayName();
        }
        
        @Override
        public boolean isApplicable(
                Class<? extends hudson.model.AbstractProject> type)
        {
            return true;
        }
    }
    
    @org.kohsuke.stapler.DataBoundConstructor
    public GprbuildBuilder()
    {
        installationName_ = "";
        proj_ = "";
        switches_ = "";
        names_ = "";
    }
    
    public String getInstallationName()
    {
        return installationName_;
    }
    
    public String getNames()
    {
        return names_;
    }
    
    public String getProj()
    {
        return proj_;
    }
    
    @Override
    public hudson.tasks.BuildStepMonitor getRequiredMonitorService()
    {
        return hudson.tasks.BuildStepMonitor.NONE;
    }
    
    public String getSwitches()
    {
        return switches_;
    }
        
    @Override
    public void perform(
            hudson.model.Run<?, ?> run,
            hudson.FilePath filePath,
            hudson.Launcher launcher,
            hudson.model.TaskListener taskListener) throws
            InterruptedException, java.io.IOException
    {            
        final hudson.util.ArgumentListBuilder args = 
                new hudson.util.ArgumentListBuilder();
        
        final java.io.File binDirectory = GnatInstallation.
                getBinDirectoryByName(installationName_);
        
        if (null == binDirectory) {
            throw new hudson.AbortException(
                    Messages.GprbuildBuilder_ExecutableMissing());
        }
        
        final java.io.File gprbuildExecutable =
                new java.io.File(binDirectory, launcher.isUnix() ?
                        GPRBUILD_FILE : GPRBUILD_FILE + EXE_EXTENSION);

        if (!gprbuildExecutable.isFile()) {
            throw new hudson.AbortException(
                    Messages.GprbuildBuilder_ExecutableMissing());
        }

        args.add(gprbuildExecutable);
            
        if (null != proj_ && !proj_.isEmpty()) {
            args.add(proj_);
        }

        if (null != switches_ && !switches_.isEmpty()) {
            args.addTokenized(switches_);
        }

        if (null != names_ && !names_.isEmpty()) {
            args.addTokenized(names_);
        }
        
        // Prepend path to GNAT installation to PATH environment variable
        // such that gprbuild finds gprconfig.
        final hudson.EnvVars envs = run.getEnvironment(taskListener);
        envs.override("PATH+", binDirectory.getAbsolutePath());
        
        final int status = launcher.launch().
                cmds(launcher.isUnix() ? args : args.toWindowsCommand()).
                envs(envs).
                stdout(taskListener).
                pwd(filePath).
                join();
        
        if (STATUS_SUCCESS != status) {
            run.setResult(hudson.model.Result.FAILURE);
            throw new hudson.AbortException(
                    Messages.GprbuildBuilder_BuildFailed(status));
        }
    }
    
    @org.kohsuke.stapler.DataBoundSetter
    public void setInstallationName(String installationName)
    {
        installationName_ = installationName;
    }
    
    @org.kohsuke.stapler.DataBoundSetter
    public void setNames(String names)
    {
        names_ = replaceNonSpaceWhitespaceWithSpace(names);
    }
    
    @org.kohsuke.stapler.DataBoundSetter
    public void setProj(String proj)
    {
        proj = replaceNonSpaceWhitespaceWithSpace(proj);
        
        if (null != proj && !proj.isEmpty() && !proj.endsWith(GPR_EXTENSION)) {
            proj = proj + GPR_EXTENSION;
        }
        
        proj_ = proj;
    }
    
    @org.kohsuke.stapler.DataBoundSetter
    public void setSwitches(String switches)
    {
        switches_ = replaceNonSpaceWhitespaceWithSpace(switches);
    }
        
    private static String replaceNonSpaceWhitespaceWithSpace(String s)
    {
        return s.replaceAll(NON_SPACE_WHITESPACE_REGEX, SPACE).trim();
    }
    
    private static final String EXE_EXTENSION = ".exe";
    private static final String GPR_EXTENSION = ".gpr";
    private static final String GPRBUILD_FILE = "gprbuild";
    private static final String NON_SPACE_WHITESPACE_REGEX =
            "[\\t\\n\\x0B\\f\\r]+";
    private static final String SPACE = " ";
    private static final int STATUS_SUCCESS = 0;
    
    private String installationName_;
    private String names_;
    private String proj_;
    private String switches_;
}