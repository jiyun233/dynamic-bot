@echo off
cd /d "%~dp0.."
java -Xms512m -Xmx2g -jar lib\dynamic-bot-4.0.0-STANDALONE.jar
pause