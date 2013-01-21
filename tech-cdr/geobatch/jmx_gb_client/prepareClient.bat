REM  script to import libs into the client lib/ folder
REM  
REM  Copyright (C) 2007,2011 GeoSolutions S.A.S.
REM  http://www.geo-solutions.it
REM
REM Permission is hereby granted, free of charge, to any person obtaining a copy
REM of this software and associated documentation files (the "Software"), to deal
REM in the Software without restriction, including without limitation the rights
REM to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
REM copies of the Software, and to permit persons to whom the Software is
REM furnished to do so, subject to the following conditions:
REM 
REM The above copyright notice and this permission notice shall be included in
REM all copies or substantial portions of the Software.
REM 
REM THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
REM IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
REM FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
REM AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
REM LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
REM OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
REM THE SOFTWARE.


SET M2_REPO=C:\Users\Cancellieri\.m2\
SET VERSION=1.3
mkdir  client-%VERSION%\
mkdir  client-%VERSION%\lib

copy %M2_REPO%\repository\it\geosolutions\jmx-geobatch-client\%VERSION%-SNAPSHOT\jmx-geobatch-client-%VERSION%-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\geobatch\gb-core-impl\1.3-SNAPSHOT\gb-core-impl-1.3-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\geobatch\gb-core-model\1.3-SNAPSHOT\gb-core-model-1.3-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\commons-collections\commons-collections\3.1\commons-collections-3.1.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\commons-io\commons-io\2.1\commons-io-2.1.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\com\thoughtworks\xstream\xstream\1.3.1\xstream-1.3.1.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\tools\tools-io\1.1-SNAPSHOT\tools-io-1.1-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\geobatch\services\gb-jmx\1.3-SNAPSHOT\gb-jmx-1.3-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\org\slf4j\jcl-over-slf4j\1.5.11\jcl-over-slf4j-1.5.11.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\org\slf4j\slf4j-api\1.5.11\slf4j-api-1.5.11.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\log4j\log4j\1.2.16\log4j-1.2.16.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\org\slf4j\slf4j-log4j12\1.5.11\slf4j-log4j12-1.5.11.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\tools\tools-compress\1.1-SNAPSHOT\tools-compress-1.1-SNAPSHOT.jar client-%VERSION%\lib\
copy %M2_REPO%\repository\it\geosolutions\tools\tools-commons\1.1-SNAPSHOT\tools-commons-1.1-SNAPSHOT.jar client-%VERSION%\lib\
copy src\main\resources\* client-%VERSION%\
REM copy run.bat client-%VERSION%\
REM copy security.policy client-%VERSION%\
REM copy README client-%VERSION%\
REM copy jmx.properties client-%VERSION%\
REM copy connection.properties client-%VERSION%\
REM copy log4j.xml client-%VERSION%\
