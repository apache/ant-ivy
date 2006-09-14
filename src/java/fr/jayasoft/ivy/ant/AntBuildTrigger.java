package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;

import fr.jayasoft.ivy.IvyContext;
import fr.jayasoft.ivy.event.AbstractTrigger;
import fr.jayasoft.ivy.event.IvyEvent;
import fr.jayasoft.ivy.event.Trigger;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;
import fr.jayasoft.ivy.util.MessageImpl;

/**
 * Triggers an ant build on an event occurence.
 * 
 * Example of use:
 * <ant-build-trigger event="pre-resolve-dependency" filter="revision=latest.integration"
 *                    antfile="/path/to/[module]/build.xml" target="compile"/>
 * Triggers an ant build for any dependency in asked in latest.integration, just before resolving the 
 * dependency.
 * 
 * @see AntCallTrigger
 * @since 1.4
 * @author Xavier Hanin
 *
 */
public class AntBuildTrigger extends AbstractTrigger implements Trigger {
	private boolean _onlyonce = true;
	private String _target = null;
	private Collection _builds = new ArrayList();
	private String _buildFilePattern;
	private String _prefix;

	public void progress(IvyEvent event) {
		File f = getBuildFile(event);
		if (f.exists()) {
			if (_onlyonce && isBuilt(f)) {
				Message.verbose("target build file already built, skipping: "+f);
			} else {
				Ant ant = new Ant();
				Project project = (Project)IvyContext.getContext().get(IvyTask.ANT_PROJECT_CONTEXT_KEY);
				if (project == null) {
					project = new Project();
					project.init();
				}
				
				ant.setProject(project);
				ant.setTaskName("ant");
				
				ant.setAntfile(f.getAbsolutePath());
				ant.setInheritAll(false);
				String target = getTarget();
				if (target != null) {
					ant.setTarget(target);
				}
				Map atts = event.getAttributes();
				for (Iterator iter = atts.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					String value = (String) atts.get(key);
					Property p = ant.createProperty();
					p.setName(_prefix == null?key:_prefix+key);
					p.setValue(value);
				}
				
				Message.verbose("triggering build: "+f+" target="+target+" for "+event);
                MessageImpl impl = IvyContext.getContext().getMessageImpl();
                try {
                	IvyContext.getContext().setMessageImpl(null);
                	ant.execute();
                	markBuilt(f);
                } finally {
                	IvyContext.getContext().setMessageImpl(impl);
                }

				Message.debug("triggered build finished: "+f+" target="+target+" for "+event);
			}
		} else {
			Message.verbose("no build file found for dependency, skipping: "+f);
		}
	}

	private void markBuilt(File f) {
		_builds.add(f.getAbsolutePath());
	}

	private boolean isBuilt(File f) {
		return _builds.contains(f.getAbsolutePath());
	}

	private File getBuildFile(IvyEvent event) {
		return new File(IvyPatternHelper.substituteTokens(getBuildFilePattern(), event.getAttributes()));
	}

	public String getBuildFilePattern() {
		return _buildFilePattern;
	}
	
	public void setAntfile(String pattern) {
		_buildFilePattern = pattern;
	}

	public String getTarget() {
		return _target;
	}

	public void setTarget(String target) {
		_target = target;
	}

	public boolean isOnlyonce() {
		return _onlyonce;
	}

	public void setOnlyonce(boolean onlyonce) {
		_onlyonce = onlyonce;
	}

	public String getPrefix() {
		return _prefix;
	}

	public void setPrefix(String prefix) {
		_prefix = prefix;
        if (!prefix.endsWith(".")) {
            _prefix += ".";
        }
	}
}
