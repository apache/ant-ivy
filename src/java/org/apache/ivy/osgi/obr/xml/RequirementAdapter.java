/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.osgi.obr.xml;

import java.text.ParseException;

import org.apache.ivy.osgi.core.BundleInfo;
import org.apache.ivy.osgi.core.BundleRequirement;
import org.apache.ivy.osgi.filter.AndFilter;
import org.apache.ivy.osgi.filter.CompareFilter;
import org.apache.ivy.osgi.filter.CompareFilter.Operator;
import org.apache.ivy.osgi.filter.NotFilter;
import org.apache.ivy.osgi.filter.OSGiFilter;
import org.apache.ivy.osgi.util.Version;
import org.apache.ivy.osgi.util.VersionRange;

public class RequirementAdapter {

    private Version startVersion = null;

    private boolean startExclusive = false;

    private Version endVersion = null;

    private boolean endExclusive = false;

    private String type = null;

    private String name = null;

    public static void adapt(BundleInfo info, Requirement requirement)
            throws UnsupportedFilterException, ParseException {
        RequirementAdapter adapter = new RequirementAdapter();
        adapter.extractFilter(requirement.getFilter());
        adapter.adapt(info, requirement.isOptional());
    }

    private void extractFilter(OSGiFilter filter) throws UnsupportedFilterException, ParseException {
        if (filter instanceof AndFilter) {
            AndFilter andFilter = (AndFilter) filter;
            for (OSGiFilter subFilter : andFilter.getSubFilters()) {
                extractFilter(subFilter);
            }
        } else if (filter instanceof CompareFilter) {
            CompareFilter compareFilter = ((CompareFilter) filter);
            parseCompareFilter(compareFilter, false);
        } else if (filter instanceof NotFilter) {
            NotFilter notFilter = ((NotFilter) filter);
            if (notFilter.getSubFilter() instanceof CompareFilter) {
                CompareFilter compareFilter = ((CompareFilter) notFilter.getSubFilter());
                parseCompareFilter(compareFilter, true);
            }
        } else {
            throw new UnsupportedFilterException("Unsupported filter: "
                    + filter.getClass().getName());
        }
    }

    private void adapt(BundleInfo info, boolean optional) throws ParseException {
        VersionRange range = getVersionRange();
        String resolution = optional ? "optional" : null;
        if (type == null) {
            throw new ParseException("No requirement actually specified", 0);
        }
        BundleRequirement requirement = new BundleRequirement(type, name, range, resolution);
        info.addRequirement(requirement);
        if (BundleInfo.EXECUTION_ENVIRONMENT_TYPE.equals(type)) {
            info.addExecutionEnvironment(name);
        }
    }

    private VersionRange getVersionRange() {
        VersionRange range = null;
        if (startVersion != null || endVersion != null) {
            range = new VersionRange(startExclusive, startVersion, endExclusive, endVersion);
        }
        return range;
    }

    private void parseCompareFilter(CompareFilter compareFilter, boolean not)
            throws UnsupportedFilterException, ParseException {
        String att = compareFilter.getLeftValue();
        if (BundleInfo.PACKAGE_TYPE.equals(att) || BundleInfo.BUNDLE_TYPE.equals(att)
                || BundleInfo.EXECUTION_ENVIRONMENT_TYPE.equals(att) || "symbolicname".equals(att)
                || BundleInfo.SERVICE_TYPE.equals(att)) {
            if (not) {
                throw new UnsupportedFilterException(
                        "Not filter on requirement comparaison is not supported");
            }
            if (type != null) {
                throw new UnsupportedFilterException("Multiple requirement type are not supported");
            }
            if ("symbolicname".equals(att)) {
                type = BundleInfo.BUNDLE_TYPE;
            } else {
                type = att;
            }
            if (compareFilter.getOperator() != Operator.EQUALS) {
                throw new UnsupportedFilterException(
                        "Filtering is only supported with the operator '='");
            }
            name = compareFilter.getRightValue();
        } else if ("version".equals(att)) {
            String v = compareFilter.getRightValue();
            Version version;
            try {
                version = new Version(v);
            } catch (ParseException e) {
                throw new ParseException("Ill formed version: " + v, 0);
            }
            Operator operator = compareFilter.getOperator();
            if (not) {
                if (operator == Operator.EQUALS) {
                    throw new UnsupportedFilterException(
                            "Not filter on equals comparaison is not supported");
                } else if (operator == Operator.GREATER_OR_EQUAL) {
                    operator = Operator.LOWER_THAN;
                } else if (operator == Operator.GREATER_THAN) {
                    operator = Operator.LOWER_OR_EQUAL;
                } else if (operator == Operator.LOWER_OR_EQUAL) {
                    operator = Operator.GREATER_THAN;
                } else if (operator == Operator.LOWER_THAN) {
                    operator = Operator.GREATER_OR_EQUAL;
                }
            }
            if (operator == Operator.EQUALS) {
                if (startVersion != null || endVersion != null) {
                    throw new UnsupportedFilterException(
                            "Multiple version matching is not supported");
                }
                startVersion = version;
                startExclusive = false;
                endVersion = version;
                endExclusive = false;
            } else if (operator == Operator.GREATER_OR_EQUAL) {
                if (startVersion != null) {
                    throw new UnsupportedFilterException(
                            "Multiple version matching is not supported");
                }
                startVersion = version;
                startExclusive = false;
            } else if (operator == Operator.GREATER_THAN) {
                if (startVersion != null) {
                    throw new UnsupportedFilterException(
                            "Multiple version matching is not supported");
                }
                startVersion = version;
                startExclusive = true;
            } else if (operator == Operator.LOWER_OR_EQUAL) {
                if (endVersion != null) {
                    throw new UnsupportedFilterException(
                            "Multiple version matching is not supported");
                }
                endVersion = version;
                endExclusive = false;
            } else if (operator == Operator.LOWER_THAN) {
                if (endVersion != null) {
                    throw new UnsupportedFilterException(
                            "Multiple version matching is not supported");
                }
                endVersion = version;
                endExclusive = true;
            }
        } else {
            throw new UnsupportedFilterException("Unsupported attribute: " + att);
        }

    }
}
