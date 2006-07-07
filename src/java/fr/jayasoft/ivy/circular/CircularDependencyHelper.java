package fr.jayasoft.ivy.circular;

import fr.jayasoft.ivy.ModuleDescriptor;
import fr.jayasoft.ivy.ModuleRevisionId;

public class CircularDependencyHelper {

    /**
     * Returns a string representation of this circular dependency graph
     * @param descriptors in order of circular dependency
     * @return
     */
    public static String formatMessage(final ModuleRevisionId[] mrids) {
        StringBuffer buff = new StringBuffer();
        buff.append(mrids[0]);
        for (int i = 1; i < mrids.length; i++) {
            buff.append("->");
            buff.append(mrids[i]);
        }
        return buff.toString();
    }

    public static String formatMessage(final ModuleDescriptor[] descriptors) {
    	return formatMessage(toMrids(descriptors));
    }

	public static ModuleRevisionId[] toMrids(ModuleDescriptor[] descriptors) {
		ModuleRevisionId[] mrids = new ModuleRevisionId[descriptors.length];
		for (int i = 0; i < descriptors.length; i++) {
			mrids[i] = descriptors[i].getModuleRevisionId();
		}
		return mrids;
	}

}
