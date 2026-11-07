@echo off
title CrissCross - Smart Event Parking System
echo ========================================================
echo  CrissCross Smart Event Parking Management System
echo  Apple-Inspired Glassmorphic JavaFX + MySQL Desktop App
echo ========================================================
echo.

call .\mvnw.cmd javafx:run

if %ERRORLEVEL% neq 0 (
    echo.
    echo [Notice] Trying fallback jar execution...
    call .\mvnw.cmd package -DskipTests
    java -jar target\crisscross-parking-1.0.0.jar
)

pause
