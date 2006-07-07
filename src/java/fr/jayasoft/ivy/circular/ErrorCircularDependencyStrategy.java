package fr.jayasoft.ivy.circular;

import fr.jayasoft.ivy.ModuleRevisionId;

public class ErrorCircularDependencyStrategy extends AbstractCircularDependencyStrategy {
	
	private static final CircularDependencyStrategy INSTANCE = new ErrorCircularDependencyStrategy();


	public static CircularDependencyStrategy getInstance() {
		return INSTANCE;
	}
	
	private ErrorCircularDependencyStrategy() {
		super("error");
	}
	
	public void handleCircularDependency(ModuleRevisionId[] mrids) {
		throw new CircularDependencyException(mrids);
	}
}
