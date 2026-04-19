@echo off
setlocal
python "%~dp0rustc_host_wrapper.py" %*
exit /b %errorlevel%
