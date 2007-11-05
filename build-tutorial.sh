#! /bin/sh

#   Licensed to the Apache Software Foundation (ASF) under one
#   or more contributor license agreements.  See the NOTICE file
#   distributed with this work for additional information
#   regarding copyright ownership.  The ASF licenses this file
#   to you under the Apache License, Version 2.0 (the
#   "License"); you may not use this file except in compliance
#   with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
#   software distributed under the License is distributed on an
#   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#   KIND, either express or implied.  See the License for the
#   specific language governing permissions and limitations
#   under the License.


#This script run every tutorials and update the doc/tutorial/log/* files.


function run () {
	#run OUTPUT_FILE ANT_SCRIPT [TARGET...]
	#Execute ANT_SCRIPT and store the output in OUTPUT_FILE.  
	#It stops the execution of the main script if the ant script fails
	echo "Run ant $2 $3 $4 $5 > $1"
	ant -lib build/artifact	-f $2 $3 $4 $5 > $1 2>&1
	if [[ $? == 0 ]] ; then 
		echo "SUCESSFULL";
	else
		cat $1
		exit -1 ;
	fi;
}

#Make the jar available to execute the tutorials
run build/tmp.log build.xml /offline jar


#go-ivy : not logged, but run in order to check if it still run
run build/tmp.log                              src/example/go-ivy/build.xml

#hello-ivy : Quick Start - start.html
run build/tmp.log                              src/example/hello-ivy/build.xml clean-cache
run doc/tutorial/log/hello-ivy-1.txt           src/example/hello-ivy/build.xml
run doc/tutorial/log/hello-ivy-2.txt           src/example/hello-ivy/build.xml

#multiple resolvers - multiple.html
run build/tmp.log                              src/example/chained-resolvers/build.xml
run doc/tutorial/log/chained-resolvers.txt     src/example/chained-resolvers/chainedresolvers-project/build.xml


#dual
run build/tmp.log                              src/example/dual/build.xml
run doc/tutorial/log/dual.txt                  src/example/dual/project/build.xml

#Project dependancies - multi-project.html
run build/tmp.log                              src/example/dependence/build.xml
run doc/tutorial/log/dependence-standalone.txt src/example/dependence/standalone/build.xml publish
run doc/tutorial/log/dependence-depending.txt  src/example/dependence/depending/build.xml
run doc/tutorial/log/dependence-standalone-2.txt src/example/dependence/standalone/build.xml publish
run doc/tutorial/log/dependence-depending-2.txt  src/example/dependence/depending/build.xml



#configuration - Using Ivy Configuration - conf.html
run build/tmp.log                              src/example/configurations/multi-projects/myapp/build.xml clean
run build/tmp.log    						   src/example/configurations/multi-projects/filter-framework/build.xml clean clean-cache clean-local
run doc/tutorial/log/configurations-lib.txt    src/example/configurations/multi-projects/filter-framework/build.xml
run doc/tutorial/log/configurations-runcc.txt  src/example/configurations/multi-projects/myapp/build.xml
run doc/tutorial/log/configurations-runhm.txt  src/example/configurations/multi-projects/myapp/build.xml run-hm

#Update samples
cp src/example/go-ivy/build.xml doc/samples/build.xml
