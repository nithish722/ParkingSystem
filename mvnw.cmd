@echo off
setlocal

set "MVN_PATH=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MVN_PATH%" (
    call "%MVN_PATH%" %*
) else (
    where mvn >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        mvn %*
    ) else (
        echo [ERROR] Maven not found! Please check Java/Maven installation.
        exit /b 1
    )
)
