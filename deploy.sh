export PROJ_HOME=/Users/jemy/Developer/JavaProjects/qiniu-logging-plugin
export VERSION=1.0.0
cd $PROJ_HOME/log4j && mvn package 
cd $PROJ_HOME/log4j2 && mvn package
cd $PROJ_HOME/logback && mvn package
cp $PROJ_HOME/log4j/target/qiniu-logging-plugin-log4j-$VERSION.jar $PROJ_HOME/deploy
cp $PROJ_HOME/log4j2/target/qiniu-logging-plugin-log4j2-$VERSION.jar $PROJ_HOME/deploy
cp $PROJ_HOME/logback/target/qiniu-logging-plugin-logback-$VERSION.jar $PROJ_HOME/deploy