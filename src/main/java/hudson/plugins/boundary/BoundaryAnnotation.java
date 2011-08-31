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
        boundary.post(build.getProject().getName(), build.getResult().toString(), Integer.toString(build.getNumber()), listener);
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
