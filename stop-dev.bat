@echo off
chcp 65001 >nul
echo ============================================
echo   停止基础服务 (MySQL/Neo4j/Grobid)
echo ============================================
docker compose stop mysql neo4j grobid
echo 基础服务已停止!
echo 如需完全清理,请执行: docker compose down
pause
