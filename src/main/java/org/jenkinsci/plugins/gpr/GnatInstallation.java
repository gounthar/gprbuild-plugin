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
 * Manage GNAT installations.
 * 
 * A directory is considered a GNAT installation directory if it contains
 * a lib and a bin directory.
 * 
 * The individual installations are stored persistent.
 * 
 * @author Felix Patschkowski
 */
public class GnatInstallation extends hudson.tools.ToolInstallation implements
        hudson.slaves.NodeSpecific<GnatInstallation>,
        hudson.model.EnvironmentSpecific<GnatInstallation> {
    
    @hudson.Extension
    public static final class DescriptorImpl extends
            hudson.tools.ToolDescriptor<GnatInstallation> {
        
        public DescriptorImpl()
        {
            installations_ = new java.util.ArrayList<>();
            load();
        }
        
        @Override
        public String getDisplayName()
        {
            return Messages.GnatInstallation_DisplayName();
        }
        
        @Override
        public GnatInstallation[] getInstallations()
        {
            return installations_.toArray(
                    new GnatInstallation[installations_.size()]);
        }
        
        @Override
        public void setInstallations(GnatInstallation... installations)
        {
            java.util.List<GnatInstallation> list = new java.util.ArrayList<>();
            
            if (null != installations) {                
                for (GnatInstallation installation : installations) {
                    if (null != hudson.Util.fixEmptyAndTrim(
                            installation.getName())) {
                        list.add(installation);
                    }
                }
            }
            
            installations_ = list;
            save();
        }
    
        @Override
        protected hudson.util.FormValidation checkHomeDirectory(
                java.io.File file)
        {
            final java.io.File libDirectory = new java.io.File(file,
                    LIB_DIRECTORY);
            final java.io.File binDirectory = new java.io.File(file,
                    BIN_DIRECTORY);
            
            return binDirectory.exists() && libDirectory.exists() ?
                    hudson.util.FormValidation.ok():
                    hudson.util.FormValidation.error(
                            Messages.GnatInstallation_NotGnatDirectory(file));
        }
        
        private java.util.List<GnatInstallation> installations_;
    }
    
    public static GnatInstallation[] allInstallations()
    {
        return jenkins.model.Jenkins.getInstance().
                getDescriptorByType(DescriptorImpl.class).
                getInstallations();
    }
           
    public static java.io.File getBinDirectoryByName(String name)
    {
        java.io.File binDirectory = null;
        
        for (GnatInstallation installation : allInstallations()) {
            if (installation.getName().equals(name)) {
                binDirectory = new java.io.File(installation.getHome(),
                        BIN_DIRECTORY);
                break;
            }
        }
        
        return binDirectory;
    }
    
    @org.kohsuke.stapler.DataBoundConstructor
    public GnatInstallation(String name, String home,
            java.util.List<? extends hudson.tools.ToolProperty<?>> properties)
    {
        super(hudson.Util.fixEmptyAndTrim(name), 
                hudson.Util.fixEmptyAndTrim(home),
                properties);
    }
    
    @Override
    public GnatInstallation forEnvironment(hudson.EnvVars environment)
    {
        return new GnatInstallation(getName(), environment.expand(getHome()),
                getProperties().toList());
    }    
    
    @Override
    public GnatInstallation forNode(hudson.model.Node node,
            hudson.model.TaskListener tl)
            throws java.io.IOException, InterruptedException
    {
        return new GnatInstallation(getName(), translateFor(node, tl),
            getProperties().toList());
    }
    
    private static final String BIN_DIRECTORY = "bin";
    private static final String LIB_DIRECTORY = "lib";
}
