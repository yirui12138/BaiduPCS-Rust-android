@echo off
setlocal
set PYTHONDONTWRITEBYTECODE=1
if "%BAIDUPCS_TRACE_WRAPPER%"=="1" (
  echo [%date% %time%] cmd wrapper path=%~f0 script=%~dp0rustc_host_wrapper.py args=%*>>"%TEMP%\baidupcs-rust-smart-app-control-cmd-trace.log"
  where python >>"%TEMP%\baidupcs-rust-smart-app-control-cmd-trace.log" 2>&1
)
python "%~dp0rustc_host_wrapper.py" %*
exit /b %errorlevel%
