@echo off
if not defined PYTHONHOME "%~dp0python\python.exe" "%~dp0play" %*
if defined PYTHONHOME "%PYTHONHOME%\python.exe" "%~dp0play" %*
