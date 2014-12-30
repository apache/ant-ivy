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
package org.apache.ivy.osgi.filter;

import java.util.Map;

public class CompareFilter extends OSGiFilter {

    public static enum Operator {

        EQUALS("="), LOWER_THAN("<"), LOWER_OR_EQUAL("<="), GREATER_THAN(">"), GREATER_OR_EQUAL(
                ">="), APPROX("~="), PRESENT("=*");

        private String op;

        private Operator(String op) {
            this.op = op;
        }

        @Override
        public String toString() {
            return op;
        }
    }

    private Operator operator;

    private final String rightValue;

    private final String leftValue;

    private boolean substring;

    public CompareFilter(String leftValue, Operator operator, String rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.operator = operator;
        this.substring = operator == Operator.EQUALS && rightValue.contains("*");
    }

    public String getLeftValue() {
        return leftValue;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getRightValue() {
        return rightValue;
    }

    @Override
    public void append(StringBuffer builder) {
        builder.append("(");
        builder.append(leftValue);
        builder.append(operator.toString());
        builder.append(rightValue);
        builder.append(")");
    }

    @Override
    public boolean eval(Map<String, String> properties) {
        String actualValue = properties.get(leftValue);
        if (actualValue == null) {
            return false;
        }
        if (operator == Operator.PRESENT) {
            return true;
        }
        if (operator == Operator.APPROX) {
            // TODO
            return false;
        }
        if (substring) {
            // TODO
            return false;
        }
        int diff = rightValue.compareTo(actualValue);
        switch (operator) {
            case EQUALS:
                return diff == 0;
            case GREATER_THAN:
                return diff > 0;
            case GREATER_OR_EQUAL:
                return diff >= 0;
            case LOWER_OR_EQUAL:
                return diff <= 0;
            case LOWER_THAN:
                return diff < 0;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((leftValue == null) ? 0 : leftValue.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((rightValue == null) ? 0 : rightValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CompareFilter)) {
            return false;
        }
        CompareFilter other = (CompareFilter) obj;
        if (leftValue == null) {
            if (other.leftValue != null) {
                return false;
            }
        } else if (!leftValue.equals(other.leftValue)) {
            return false;
        }
        if (operator == null) {
            if (other.operator != null) {
                return false;
            }
        } else if (!operator.equals(other.operator)) {
            return false;
        }
        if (rightValue == null) {
            if (other.rightValue != null) {
                return false;
            }
        } else if (!rightValue.equals(other.rightValue)) {
            return false;
        }
        return true;
    }
}
