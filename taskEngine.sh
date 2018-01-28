#!/bin/sh
#LD_LIBRARY_PATH=/usr/lib/lwp; export LD_LIBRARY_PATH
# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.
 
set -a
set -x
 
# Initialize the common environment.
 
Today=`date '+%Y/%m/%d %H:%M:%S'`
LOGDATE=`date '+%Y%m%d'`
 
log4jProperties="./emsQueue_log4j.properties"
 
cd /wsa/WM_projects/EMSTask/
 
Lib_HOME="/wsa/WM_projects/EMSTask/";
 
JAVA_HOME="/wsa/java/jdk1.6.0_20"
 
LC_ALL=zh_TW.BIG5
 
export LC_ALL
 
JAVA_VM="-server"
 
MEM_ARGS="-Xms64m -Xmx256m"
 
CLASSPATH="${CLASSPATH}:${Lib_HOME}/cglib-nodep-2.1_3.jar:${Lib_HOME}/commons-beanutils-1.7.0.jar:${Lib_HOME}/commons-codec-1.3.jar:${Lib_HOME}/commons-collections-3.1.jar:${Lib_HOME}/commons-dbcp.jar:${Lib_HOME}/commons-digester-2.0.jar:${Lib_HOME}/commons-fileupload-1.2.1.jar:${Lib_HOME}/commons-httpclient-3.0.jar:${Lib_HOME}/commons-io-2.0.1.jar:${Lib_HOME}/commons-lang-2.5.jar:${Lib_HOME}/commons-logging-1.0.4.jar:${Lib_HOME}/commons-pool.jar:${Lib_HOME}/commons-validator-1.3.1.jar:${Lib_HOME}/dom4j-1.6.1.jar:${Lib_HOME}/jaxen-1.1.1.jar:${Lib_HOME}/hsqldb.jar:${Lib_HOME}/log4j-1.2.13.jar:${Lib_HOME}/ojdbc14.jar:${Lib_HOME}/quartz-1.8.6.jar:${Lib_HOME}/slf4j-api-1.4.3.jar:${Lib_HOME}/slf4j-log4j12-1.4.3.jar:${Lib_HOME}/spring.jar:${Lib_HOME}/spring-asm-3.0.5.RELEASE.jar:${Lib_HOME}/spring-beans-3.0.5.RELEASE.jar:${Lib_HOME}/spring-context-3.0.5.RELEASE.jar:${Lib_HOME}/spring-context-support-3.0.5.RELEASE.jar:${Lib_HOME}/spring-core-3.0.5.RELEASE.jar:${Lib_HOME}/spring-expression-3.0.5.RELEASE.jar:${Lib_HOME}/spring-jdbc-3.0.5.RELEASE.jar:${Lib_HOME}/spring-tx-3.0.5.RELEASE.jar:${Lib_HOME}/spring-web-3.0.5.RELEASE.jar:${Lib_HOME}/spring-web-3.1.2.RELEASE.jar:${Lib_HOME}/mail-1.4.7.jar"
 
export CLASSPATH
 
SERVICE_NAME=TaskEngine
PATH_TO_JAR=/wsa/WM_projects/EMSTask/TaskEngine.jar
PID_PATH_NAME=/tmp/TaskEngine-pid
 
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup ${JAVA_HOME}/bin/java ${JAVA_VM} ${MEM_ARGS} ${JAVA_OPTIONS} -jar -Dlog4j.configuration=file:${log4jProperties} $PATH_TO_JAR /tmp 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup ${JAVA_HOME}/bin/java ${JAVA_VM} ${MEM_ARGS} ${JAVA_OPTIONS} -jar -Dlog4j.configuration=file:${log4jProperties} $PATH_TO_JAR /tmp 2>> /dev/null >> /dev/null &
                        echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
        checkStatus)
        
          PID= $(pgrep -f TaskEngine);
                  
                  if [! -f $PID ]; then
                        echo "$SERVICE_NAME stopping ...";
                  
                  
                  fi
    ;;
esac