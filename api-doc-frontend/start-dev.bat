@echo off
chcp 65001 >nul
REM API Document Manager - 前端开发环境启动脚本 (Windows)

echo ==========================================
echo   API Document Manager - 前端开发环境
echo ==========================================
echo.

REM 检查是否已安装pnpm
where pnpm >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装 pnpm
    echo 请先安装 pnpm: npm install -g pnpm
    pause
    exit /b 1
)

REM 检查node_modules是否存在
if not exist "node_modules" (
    echo 首次运行，正在安装依赖...
    echo.
    call pnpm install
    if %errorlevel% neq 0 (
        echo.
        echo pnpm install 失败，尝试使用 npm...
        call npm install
    )
) else (
    echo 依赖已安装，跳过安装步骤
)

echo.
echo 正在启动开发服务器...
echo 访问地址: http://localhost:5173
echo.
echo 按 Ctrl+C 停止服务器
echo.

REM 启动开发服务器
call pnpm dev

pause
