"cd /d %~dp0"
set LIB=../lib
set CONF=../conf/oxd-server.yml
echo CONF=%CONF%
start /b javaw -Djava.net.preferIPv4Stack=true -cp %LIB%/oxd-server.jar;%LIB%/* org.gluu.oxd.server.OxdServerApplication server %CONF%
