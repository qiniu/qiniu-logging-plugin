export PROJ_HOME=/Users/jemy/Developer/JavaProjects/qiniu-logging-plugin
export VERSION=1.0.0

# mkdir 
mkdir -p $PROJ_HOME/deploy/log4j/
mkdir -p $PROJ_HOME/deploy/log4j2/
mkdir -p $PROJ_HOME/deploy/logback/
mkdir -p $PROJ_HOME/deploy/output/

# mvn package
cd $PROJ_HOME/log4j   && mvn package 
cd $PROJ_HOME/log4j2  && mvn package
cd $PROJ_HOME/logback && mvn package

# collect jars
cp $PROJ_HOME/log4j/target/qiniu-logging-plugin-log4j-$VERSION.jar     $PROJ_HOME/deploy/log4j/
cp $PROJ_HOME/log4j2/target/qiniu-logging-plugin-log4j2-$VERSION.jar   $PROJ_HOME/deploy/log4j2/
cp $PROJ_HOME/logback/target/qiniu-logging-plugin-logback-$VERSION.jar $PROJ_HOME/deploy/logback/

# tar gz packages
cd $PROJ_HOME/deploy/log4j/   && tar czvf qiniu-logging-plugin-log4j-$VERSION.tar.gz *.jar
cd $PROJ_HOME/deploy/log4j2/  && tar czvf qiniu-logging-plugin-log4j2-$VERSION.tar.gz *.jar
cd $PROJ_HOME/deploy/logback/ && tar czvf qiniu-logging-plugin-logback-$VERSION.tar.gz *.jar

# move files to output
mv $PROJ_HOME/deploy/log4j/qiniu-logging-plugin-log4j-$VERSION.tar.gz     $PROJ_HOME/deploy/output/
mv $PROJ_HOME/deploy/log4j2/qiniu-logging-plugin-log4j2-$VERSION.tar.gz   $PROJ_HOME/deploy/output/
mv $PROJ_HOME/deploy/logback/qiniu-logging-plugin-logback-$VERSION.tar.gz $PROJ_HOME/deploy/output/