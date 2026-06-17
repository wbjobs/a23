@echo off
chcp 65001 >nul
echo ============================================
echo   开发模式启动脚本 - 仅启动基础服务
echo   (后端和前端在IDE中单独启动)
echo ============================================
echo.

echo [1/3] 启动 MySQL + Neo4j + Grobid...
docker compose up -d mysql neo4j grobid

echo.
echo [2/3] 等待 MySQL 就绪...
:wait_mysql
docker exec review_mysql mysqladmin ping -uroot -proot --silent 2>nul
if errorlevel 1 (
    echo 等待 MySQL...
    timeout /t 5 /nobreak >nul
    goto wait_mysql
)
echo MySQL 就绪! (jdbc:mysql://localhost:3306/review_system  root/root)

echo.
echo [3/3] 等待 Neo4j 就绪...
:wait_neo4j
curl -s -o nul -w "%%{http_code}" http://localhost:7474 | findstr "200 401" >nul
if errorlevel 1 (
    echo 等待 Neo4j...
    timeout /t 10 /nobreak >nul
    goto wait_neo4j
)
echo Neo4j 就绪! (bolt://localhost:7687 neo4j/12345678)

echo.
echo ============================================
echo   基础服务已就绪!
echo ============================================
echo.
echo 接下来请手动启动:
echo.
echo   1. Python服务:
echo      cd python-service
echo      pip install -r requirements.txt
echo      python run.py
echo.
echo   2. Spring Boot后端 (JDK 17+):
echo      cd backend
echo      mvn spring-boot:run
echo      或在IDEA中启动 ResearchBackendApplication
echo.
echo   3. Vue3前端:
echo      cd frontend
echo      npm install
echo      npm run dev
echo.
echo Grobid 请等待约 2 分钟启动完成: http://localhost:8070
echo.
echo 停止基础服务: 执行 stop-dev.bat
echo.
pause
