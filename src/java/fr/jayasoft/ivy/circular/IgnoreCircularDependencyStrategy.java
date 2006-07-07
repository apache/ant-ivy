package fr.jayasoft.ivy.circular;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.Message;

public class IgnoreCircularDependencyStrategy extends AbstractCircularDependencyStrategy {
	
	private static final CircularDependencyStrategy INSTANCE = new IgnoreCircularDependencyStrategy();


	public static CircularDependencyStrategy getInstance() {
		return INSTANCE;
	}
	
	private IgnoreCircularDependencyStrategy() {
		super("ignore");
	}
	
	public void handleCircularDependency(ModuleRevisionId[] mrids) {
		Message.verbose("circular dependency found: "+ CircularDependencyHelper.formatMessage(mrids));
	}
}
