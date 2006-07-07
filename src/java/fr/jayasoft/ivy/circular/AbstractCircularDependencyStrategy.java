package fr.jayasoft.ivy.circular;

public abstract class AbstractCircularDependencyStrategy implements CircularDependencyStrategy {
	private String _name;

	protected AbstractCircularDependencyStrategy(String name) {
		_name = name;	
	}
	
	public String getName() {
		return _name;
	}
	
	public String toString() {
		return getName();
	}
}
