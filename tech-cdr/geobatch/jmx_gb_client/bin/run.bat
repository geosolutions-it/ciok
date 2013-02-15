REM GEOSOLUTIONS S.A.S. 
REM batch script edited by Emanuele Notarnicola 24/05/2012 
REM for info mail to: emanuele.notarnicola@geo-solutions.it
echo off 
cls
echo.
echo #######################################################################################################################
echo ################### Welcome into JMX_client Batch Program some GeoTiff or ShpFiles will be published! #################
echo #######################################################################################################################
echo.
echo USAGE: %0 command.properties out.xml err.xml
echo. 

java -Dfile.encoding=CP-1252 -Djava.security.manager -Djava.security.policy=security.policy -classpath %CLASSPATH%;lib\* it.geosolutions.geobatch.services.jmx.MainGeoBatchJMXClient %1 %2 %3
















