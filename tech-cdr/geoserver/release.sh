#!/bin/bash

if [ $# -eq 2 -a "$2" == "Y" ]; then
	TAG=$1;
	echo "Performing TAG: "$TAG
else
	echo "USAGE: "$0 TAG Y
	exit 1;
fi

#git pull origin master
#./changeVersion -c . ${TAG}
#nano pom.xml
#git add -u
#git commit -m "release ${TAG}"
#git tag -a ${TAG} -m "release ${TAG}"
#try out -> git svn rebase
#git svn rebase
#git svn dcommit
#git svn tag ${TAG} -m "release ${TAG}"

#check tag here https://svn.fao.org/projects/techcdr/faodata-core/geosolutions/tags/

#./changeVersion -c . tech-cdr-geoserver-2.2-SNAPSHOT
#nano pom.xml
#git add -u
#git commit -m "preparing for the next tech-cdr geoserver release iteration"
#git svn rebase
#git svn dcommit
#git push origin master --tags
