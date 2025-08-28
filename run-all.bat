@echo off
REM ------------------------------
REM Скрипт запуска всех Spring Boot сервисов
REM ------------------------------

echo Запуск service-main...
start cmd /k "cd service-main && mvn spring-boot:run"

echo Запуск service-history...
start cmd /k "cd service-history && mvn spring-boot:run"

echo Запуск service-compression...
start cmd /k "cd service-compression && mvn spring-boot:run"

echo Запуск service-streaming...
start cmd /k "cd service-streaming && mvn spring-boot:run"

echo Все сервисы запущены.
pause
