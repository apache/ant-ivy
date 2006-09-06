package fr.jayasoft.ivy.util;

import fr.jayasoft.ivy.IvyContext;

/**
 * A simple thread subclass associated the same IvyContext as the thread in which it is instanciated.
 * 
 * If you override the run target, then you will have to call initContext() to do the association
 * with the original IvyContext.
 * 
 * @see IvyContext
 * @author Xavier Hanin
 */
public class IvyThread extends Thread {
	private IvyContext _context = IvyContext.getContext(); 

	public IvyThread() {
		super();
	}

	public IvyThread(Runnable target, String name) {
		super(target, name);
	}

	public IvyThread(Runnable target) {
		super(target);
	}

	public IvyThread(String name) {
		super(name);
	}

	public IvyThread(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
	}

	public IvyThread(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	public IvyThread(ThreadGroup group, Runnable target) {
		super(group, target);
	}

	public IvyThread(ThreadGroup group, String name) {
		super(group, name);
	}

	public void run() {
		initContext();
		super.run();
	}

	protected void initContext() {
		IvyContext.setContext(_context);
	}
}
