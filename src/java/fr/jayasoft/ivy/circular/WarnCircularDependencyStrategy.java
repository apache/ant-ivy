package fr.jayasoft.ivy.circular;

import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.util.Message;

public class WarnCircularDependencyStrategy extends AbstractCircularDependencyStrategy {
	
	private static final CircularDependencyStrategy INSTANCE = new WarnCircularDependencyStrategy();


	public static CircularDependencyStrategy getInstance() {
		return INSTANCE;
	}
	
	private WarnCircularDependencyStrategy() {
		super("warn");
	}
	
	public void handleCircularDependency(ModuleRevisionId[] mrids) {
		Message.warn("circular dependency found: "+ CircularDependencyHelper.formatMessage(mrids));
	}
}
