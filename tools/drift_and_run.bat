@echo off
chcp 65001 > nul

if "%~1"=="" (
    cls
    echo.
    echo 请将 .mdtc 或 .mdtcode 文件拖入本窗口，然后按回车。
    echo.
    set /p "files=文件路径: "
    call "%~f0" %files%
    exit /b
)

java -jar mdtc.jar -i %* -f

echo.
echo Process done.
pause