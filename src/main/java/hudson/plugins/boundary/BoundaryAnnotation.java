/*
* Author:: Joe Williams (j@boundary.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/

/*
* some of this was based on https://github.com/jenkinsci/hudson-notifo-plugin
*/

package hudson.plugins.boundary;

import com.google.common.base.Splitter;

import hudson.Extension;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class BoundaryAnnotation extends Notifier {

    public final String serviceUser;
    public final String apiToken;
    private String json;
    private transient Boundary boundary;

    @DataBoundConstructor
    public BoundaryAnnotation(String serviceUser, String apiToken) {
        this.serviceUser = serviceUser;
        this.apiToken = apiToken;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private void initializeBoundary()
            throws IOException {
        if (boundary == null) {
            boundary = new Boundary(this.serviceUser, this.apiToken);
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        initializeBoundary();
        boundary.annotate(build);
        return true;
    }

    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {
        /*
         * (non-Javadoc)
         *
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "Boundary Annotations";
        }
    }
}
