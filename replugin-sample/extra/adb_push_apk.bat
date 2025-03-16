:: adb_push_apk.bat
CHCP 65001

@ECHO OFF

SET CURRENT=%~dp0
SET TARGET=/sdcard/Download

adb wait-for-device && adb devices && adb root && adb remount 

ECHO "adb_push_apk.bat prev %TARGET%/*.apk"
adb shell ls -ahl  %TARGET%/*.apk 

ECHO "adb_push_apk.bat begin push *.apk"
:: FOR /r 表示递归搜索当前目录及其所有子目录。
:: (*.*) 匹配所有文件（包括隐藏文件），如果你只想看普通文件，可以改为.+。
SETLOCAL enabledelayedexpansion
FOR /f %%i in ('dir /b /s %CURRENT%\*.apk') do (
::FOR /r "%CURRENT%" %%i in (*.apk) do (
	ECHO %%i
	:: 文件替换
	adb push %%i %TARGET%
)

ECHO "adb_push_apk.bat post %TARGET%/*.apk"
adb shell ls -ahl  %TARGET%/*.apk 

SET RET=%ERRORLEVEL%
ECHO "adb_push_apk.bat return %RET%"


PAUSE

EXIT /b  %RET%


