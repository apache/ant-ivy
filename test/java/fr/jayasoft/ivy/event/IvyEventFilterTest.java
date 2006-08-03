package fr.jayasoft.ivy.event;

import java.util.Date;

import junit.framework.TestCase;
import fr.jayasoft.ivy.DefaultModuleDescriptor;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;
import fr.jayasoft.ivy.event.resolve.EndResolveEvent;
import fr.jayasoft.ivy.event.resolve.StartResolveEvent;
import fr.jayasoft.ivy.report.ResolveReport;

public class IvyEventFilterTest extends TestCase {
	Ivy ivy = new Ivy();
	ModuleDescriptor md = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo", "bar", "1.0"), "integration", new Date());
	ModuleDescriptor md2 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo2", "bar", "1.0"), "integration", new Date());
	ModuleDescriptor md3 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo3", "baz", "1.0"), "integration", new Date());
	ModuleDescriptor md4 = new DefaultModuleDescriptor(ModuleRevisionId.newInstance("foo", "baz", "1.0"), "integration", new Date());

	public void testSimple() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", null, null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new EndResolveEvent(ivy, md, new String[] {"default"}, new ResolveReport(md))));
	}
	
	public void testSimpleExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "organisation = foo, foo2", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testAndExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo AND module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));

		f = new IvyEventFilter("pre-resolve", "organisation = foo,foo2 AND module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testOrExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "organisation = foo3 OR module = bar", null);
		
		assertTrue(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}

	public void testNotExpression() {
		IvyEventFilter f = new IvyEventFilter("pre-resolve", "NOT organisation = foo", null);
		
		assertFalse(f.accept(new StartResolveEvent(ivy, md, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md2, new String[] {"default"})));
		assertTrue(f.accept(new StartResolveEvent(ivy, md3, new String[] {"default"})));
		assertFalse(f.accept(new StartResolveEvent(ivy, md4, new String[] {"default"})));
	}
}
