@echo off
chcp 65001 >nul
echo ============================================
echo   多模态知识图谱驱动的智能论文评审系统
echo   启动脚本 (Windows)
echo ============================================
echo.

echo [1/6] 正在启动 MySQL + Neo4j + Grobid 基础设施...
docker compose up -d mysql neo4j grobid
if errorlevel 1 (
    echo 错误: 基础服务启动失败
    pause
    exit /b 1
)

echo.
echo [2/6] 等待 MySQL 就绪...
:wait_mysql
docker exec review_mysql mysqladmin ping -uroot -proot --silent 2>nul
if errorlevel 1 (
    echo 等待 MySQL...
    timeout /t 5 /nobreak >nul
    goto wait_mysql
)
echo MySQL 已就绪!

echo.
echo [3/6] 等待 Neo4j 就绪...
:wait_neo4j
curl -s -o nul -w "%%{http_code}" http://localhost:7474 | findstr "200 401" >nul
if errorlevel 1 (
    echo 等待 Neo4j...
    timeout /t 10 /nobreak >nul
    goto wait_neo4j
)
echo Neo4j 已就绪!

echo.
echo [4/6] 等待 Grobid 就绪 (首次启动约需 2 分钟)...
:wait_grobid
curl -s -o nul -w "%%{http_code}" http://localhost:8070/api/processHeaderDocument 2>nul | findstr "200 405" >nul
if errorlevel 1 (
    echo 等待 Grobid...
    timeout /t 15 /nobreak >nul
    goto wait_grobid
)
echo Grobid 已就绪!

echo.
echo [5/6] 正在启动 Python PDF 解析服务...
docker compose up -d python-service

echo.
echo [6/6] 正在启动后端服务 + 前端服务...
docker compose up -d backend frontend

echo.
echo ============================================
echo   所有服务启动中,请稍候几分钟!
echo ============================================
echo.
echo 服务地址:
echo   前端页面:    http://localhost:80
echo   后端API:     http://localhost:8080
echo   Python服务:  http://localhost:5000
echo   Neo4j界面:   http://localhost:7474 (neo4j/12345678)
echo   Grobid:      http://localhost:8070
echo.
echo 测试账号:
echo   管理员 admin / admin123
echo   作者   author1 / author123
echo   评审人 reviewer1 / reviewer123
echo.
echo 查看服务状态: docker compose ps
echo 查看日志:     docker compose logs -f [服务名]
echo 停止所有:     docker compose down
echo.
pause
