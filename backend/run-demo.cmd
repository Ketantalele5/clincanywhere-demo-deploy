@echo off
setlocal
cd /d "%~dp0"
rem Restart our healthy local API on repeat runs to avoid H2 file lock errors.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0restart-demo-api.ps1"
if errorlevel 1 exit /b 1
set "SPRING_PROFILES_ACTIVE=dev"
set "JWT_SECRET=Y2FyZXBpbG90LWxvY2FsLWRlbW8tc2VjcmV0LWtleS1ub3QtZm9yLXByb2Q="
set "DB_URL=jdbc:h2:file:./data/clincanywhere-demo;DB_CLOSE_ON_EXIT=FALSE"
set "DB_USERNAME=sa"
set "DB_PASSWORD="
set "DB_DRIVER=org.h2.Driver"
call mvn spring-boot:run
endlocal
