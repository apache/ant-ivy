package fr.jayasoft.ivy.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.tools.ant.taskdefs.Ant;

import fr.jayasoft.ivy.event.AbstractTrigger;
import fr.jayasoft.ivy.event.IvyEvent;
import fr.jayasoft.ivy.event.Trigger;
import fr.jayasoft.ivy.util.IvyPatternHelper;
import fr.jayasoft.ivy.util.Message;

/**
 * Triggers an ant build on an event occurence.
 * 
 * Example of use:
 * <ant-build-trigger event="pre-resolve-dependency" filter="revision=latest.integration"
 *                    antfile="/path/to/[module]/build.xml" target="compile"/>
 * Triggers an ant build for any dependency in asked in latest.integration, just before resolving the 
 * dependency.
 * 
 * @author Xavier Hanin
 *
 */
public class AntBuildTrigger extends AbstractTrigger implements Trigger {
	private boolean _onlyonce = true;
	private String _target = null;
	private Collection _builds = new ArrayList();
	private String _buildFilePattern;

	public void progress(IvyEvent event) {
		File f = getBuildFile(event);
		if (f.exists()) {
			if (_onlyonce && isBuilt(f)) {
				Message.verbose("dependency already built, skipping: "+f);
			} else {
				Ant ant = new Ant();
				ant.setTaskName("ant");
				ant.setAntfile(f.getAbsolutePath());
				String target = getTarget();
				if (target != null) {
					ant.setTarget(target);
				}
				ant.execute();
				markBuilt(f);
			}
		}
		Message.verbose("no build file found for dependency, skipping: "+f);
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

	private String getBuildFilePattern() {
		return _buildFilePattern;
	}
	
	private void setAntfile(String pattern) {
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
}
