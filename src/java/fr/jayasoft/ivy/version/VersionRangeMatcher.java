package fr.jayasoft.ivy.version;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jayasoft.ivy.ArtifactInfo;
import fr.jayasoft.ivy.Ivy;
import fr.jayasoft.ivy.IvyAware;
import fr.jayasoft.ivy.LatestStrategy;
import fr.jayasoft.ivy.ModuleRevisionId;

/**
 * Matches version ranges:
 * [1.0,2.0] matches all versions greater or equal to 1.0 and lower or equal to 2.0
 * [1.0,2.0[ matches all versions greater or equal to 1.0 and lower than 2.0
 * ]1.0,2.0] matches all versions greater than 1.0 and lower or equal to 2.0
 * ]1.0,2.0[ matches all versions greater than 1.0 and lower than 2.0
 * [1.0,) matches all versions greater or equal to 1.0
 * ]1.0,) matches all versions greater than 1.0
 * (,2.0] matches all versions lower or equal to 2.0
 * (,2.0[ matches all versions lower than 2.0
 * 
 * This class uses a latest strategy to compare revisions.
 * If none is set, it uses the default one of the ivy instance set through setIvy().
 * If neither a latest strategy nor a ivy instance is set, an IllegalStateException
 * will be thrown when calling accept().
 * 
 * Note that it can't work with latest time strategy, cause no time is known for the limits of the range.
 * Therefore only purely revision based LatestStrategy can be used.  
 * 
 * @author xavier hanin
 *
 */
public class VersionRangeMatcher   extends AbstractVersionMatcher implements IvyAware {
	// todo: check these constants
	private final static String OPEN_INC = "[";
	private final static String OPEN_EXC = "]";
	private final static String CLOSE_INC = "]";
	private final static String CLOSE_EXC = "[";
	private final static String LOWER_INFINITE = "(";
	private final static String UPPER_INFINITE = ")";
	private final static String SEPARATOR = ",";

	// following patterns are built upon constants above and should not be modified
	private final static String OPEN_INC_PATTERN = "\\"+OPEN_INC;
	private final static String OPEN_EXC_PATTERN = "\\"+OPEN_EXC;
	private final static String CLOSE_INC_PATTERN = "\\"+CLOSE_INC;
	private final static String CLOSE_EXC_PATTERN = "\\"+CLOSE_EXC;
	private final static String LI_PATTERN = "\\"+LOWER_INFINITE;
	private final static String UI_PATTERN = "\\"+UPPER_INFINITE;
	private final static String SEP_PATTERN = "\\"+SEPARATOR;

	private final static String OPEN_PATTERN = "["+OPEN_INC_PATTERN+OPEN_EXC_PATTERN+"]";
	private final static String CLOSE_PATTERN = "["+CLOSE_INC_PATTERN+CLOSE_EXC_PATTERN+"]";
	private final static String ANY_NON_SPECIAL_PATTERN = "[^"+SEP_PATTERN+OPEN_INC_PATTERN+OPEN_EXC_PATTERN+CLOSE_INC_PATTERN+CLOSE_EXC_PATTERN+LI_PATTERN+UI_PATTERN+"]";
	
	private final static String FINITE_PATTERN = OPEN_PATTERN+"("+ANY_NON_SPECIAL_PATTERN+"+)"+SEP_PATTERN+"("+ANY_NON_SPECIAL_PATTERN+"+)"+CLOSE_PATTERN;
	private final static String LOWER_INFINITE_PATTERN = LI_PATTERN+"\\,("+ANY_NON_SPECIAL_PATTERN+"+)"+CLOSE_PATTERN;
	private final static String UPPER_INFINITE_PATTERN = OPEN_PATTERN+"("+ANY_NON_SPECIAL_PATTERN+"+)\\,"+UI_PATTERN;
	
	private final static Pattern FINITE_RANGE = Pattern.compile(FINITE_PATTERN);
	private final static Pattern LOWER_INFINITE_RANGE = Pattern.compile(LOWER_INFINITE_PATTERN);
	private final static Pattern UPPER_INFINITE_RANGE = Pattern.compile(UPPER_INFINITE_PATTERN);
	private final static Pattern ALL_RANGE = Pattern.compile(FINITE_PATTERN+"|"+LOWER_INFINITE_PATTERN+"|"+UPPER_INFINITE_PATTERN);
	
	private final class MRIDArtifactInfo implements ArtifactInfo {
		private ModuleRevisionId _mrid;

		public MRIDArtifactInfo(ModuleRevisionId id) {
			_mrid = id;
		}

		public long getLastModified() {
			return 0;
		}

		public String getRevision() {
			return _mrid.getRevision();
		}
	}

	private final Comparator COMPARATOR = new Comparator() {
		public int compare(Object o1, Object o2) {
			if (o1.equals(o2)) {
				return 0;
			}
			ArtifactInfo art1 = new MRIDArtifactInfo((ModuleRevisionId)o1);
			ArtifactInfo art2 = new MRIDArtifactInfo((ModuleRevisionId)o2);
			ArtifactInfo art = getLatestStrategy().findLatest(new ArtifactInfo[] {art1,art2}, null);
			return art == art1 ? -1 : 1;
		}
	};
	

	private LatestStrategy _latestStrategy;
	
	private String _latestStrategyName = "default";

	public VersionRangeMatcher() {
		super("version-range");
	}

	public VersionRangeMatcher(String name) {
		super(name);
	}

	public VersionRangeMatcher(String name, LatestStrategy strategy) {
		super(name);
		_latestStrategy = strategy;
	}

	public VersionRangeMatcher(String name, Ivy ivy) {
		super(name);
		setIvy(ivy);
	}

	public boolean isDynamic(ModuleRevisionId askedMrid) {
		return ALL_RANGE.matcher(askedMrid.getRevision()).matches();
	}

	public boolean accept(ModuleRevisionId askedMrid, ModuleRevisionId foundMrid) {
		String revision = askedMrid.getRevision();
		Matcher m;
		m = FINITE_RANGE.matcher(revision);
		if (m.matches()) {
			String lower = m.group(1);
			String upper = m.group(2);
			return isUpper(askedMrid, lower, foundMrid, revision.startsWith(OPEN_INC))
			&& isLower(askedMrid, upper, foundMrid, revision.endsWith(CLOSE_INC));
		}
		m = LOWER_INFINITE_RANGE.matcher(revision);
		if (m.matches()) {
			String upper = m.group(1);
			return isLower(askedMrid, upper, foundMrid, revision.endsWith(CLOSE_INC));
		}
		m = UPPER_INFINITE_RANGE.matcher(revision);
		if (m.matches()) {
			String lower = m.group(1);
			return isUpper(askedMrid, lower, foundMrid, revision.startsWith(OPEN_INC));
		}
		return false;
	}

	private boolean isLower(ModuleRevisionId askedMrid, String revision, ModuleRevisionId foundMrid, boolean inclusive) {
		return COMPARATOR.compare(ModuleRevisionId.newInstance(askedMrid, revision), foundMrid) <= (inclusive ? 0 : -1);
	}

	private boolean isUpper(ModuleRevisionId askedMrid, String revision, ModuleRevisionId foundMrid, boolean inclusive) {
		return COMPARATOR.compare(ModuleRevisionId.newInstance(askedMrid, revision), foundMrid) >= (inclusive ? 0 : 1);
	}

	public LatestStrategy getLatestStrategy() {
		if (_latestStrategy == null) {
			if (getIvy() == null) {
				throw new IllegalStateException("no ivy instance nor latest strategy configured in version range matcher "+this);
			}
			if (_latestStrategyName == null) {
				throw new IllegalStateException("null latest strategy defined in version range matcher "+this);
			}
			_latestStrategy = getIvy().getLatestStrategy(_latestStrategyName);
			if (_latestStrategy == null) {
				throw new IllegalStateException("unknown latest strategy '"+_latestStrategyName+"' configured in version range matcher "+this);
			}
		}
		return _latestStrategy;
	}

	public void setLatestStrategy(LatestStrategy latestStrategy) {
		_latestStrategy = latestStrategy;
	}
	
	public void setLatest(String latestStrategyName) {
		_latestStrategyName = latestStrategyName;
	}

}
