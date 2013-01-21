#!/bin/sh
#  jmx-geobatch-client - JMX client for GeoBatch
#  
#  Copyright (C) 2007,2011 GeoSolutions S.A.S.
#  http://www.geo-solutions.it
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

M2_REPO=/home/carlo/.m2/

JMX_FILE=src/test/resources/jmx.properties

java -Dfile.encoding=UTF-8 -classpath "${M2_REPO}/repository/it/geosolutions/jmx-geobatch-client/1.3-SNAPSHOT/jmx-geobatch-client-1.3-SNAPSHOT.jar:\
${M2_REPO}/repository/commons-io/commons-io/2.1/commons-io-2.1.jar:\
${M2_REPO}/repository/it/geosolutions/geobatch/gb-core-impl/1.3-SNAPSHOT/gb-core-impl-1.3-SNAPSHOT.jar:\
${M2_REPO}/repository/com/thoughtworks/xstream/xstream/1.3.1/xstream-1.3.1.jar:\
${M2_REPO}/repository/it/geosolutions/tools/tools-io/1.1-SNAPSHOT/tools-io-1.1-SNAPSHOT.jar:\
${M2_REPO}/repository/it/geosolutions/geobatch/services/gb-jmx/1.3-SNAPSHOT/gb-jmx-1.3-SNAPSHOT.jar:\
${M2_REPO}/repository/org/slf4j/jcl-over-slf4j/1.5.11/jcl-over-slf4j-1.5.11.jar:\
${M2_REPO}/repository/org/slf4j/slf4j-api/1.5.11/slf4j-api-1.5.11.jar:\
${M2_REPO}/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:\
${M2_REPO}/repository/org/slf4j/slf4j-log4j12/1.5.11/slf4j-log4j12-1.5.11.jar" it.geosolutions.geobatch.services.jmx.MainGeoBatchJMXClient ${JMX_FILE}
