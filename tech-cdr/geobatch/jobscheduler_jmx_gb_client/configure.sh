#!/bin/sh

JOB_SCHEDULER_HOME="/opt/sos-berlin.com/jobscheduler/scheduler"
M2_REPO="/home/carlo/.m2/repository/"

cp -vfa target/jobscheduler-jmx-geobatch-1.0-SNAPSHOT.jar ${JOB_SCHEDULER_HOME}/lib/
cp -vfa ${M2_REPO}/it/geosolutions/geobatch/services/gb-jmx/1.2-SNAPSHOT/gb-jmx-1.2-SNAPSHOT.jar ${JOB_SCHEDULER_HOME}/lib/

cp -vfa ./security.policy $JOB_SCHEDULER_HOME/config/
cp -vfa ./jmx.properties $JOB_SCHEDULER_HOME/config/
echo "options                = -Xmx256m -Djava.security.policy=$JOB_SCHEDULER_HOME/config/security.policy" >> $JOB_SCHEDULER_HOME/config/sos.ini
cp -vfa ./geobatch.job.xml $JOB_SCHEDULER_HOME/config/live/

