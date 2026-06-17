@echo off
chcp 65001 >nul
echo ============================================
echo   停止所有服务
echo ============================================
docker compose down
echo 所有服务已停止!
pause
