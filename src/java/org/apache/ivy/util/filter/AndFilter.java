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
package org.apache.ivy.util.filter;

public class AndFilter implements Filter {
	private Filter _op1;
	private Filter _op2;
	
	public AndFilter(Filter op1, Filter op2) {
		_op1 = op1;
		_op2 = op2;
	}
	public Filter getOp1() {
		return _op1;
	}
	public Filter getOp2() {
		return _op2;
	}
	public boolean accept(Object o) {
		return _op1.accept(o) && _op2.accept(o);
	}
}
