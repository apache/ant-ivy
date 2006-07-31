package fr.jayasoft.ivy.event.resolve;

import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.report.ResolveReport;

public class EndResolveEvent extends ResolveEvent {
	public static final String NAME = "post-resolve";

	private ResolveReport _report;

	public EndResolveEvent(Ivy source, ModuleDescriptor md, String[] confs, ResolveReport report) {
		super(source, NAME, md, confs);
		_report = report;
	}

	public ResolveReport getReport() {
		return _report;
	}

}
