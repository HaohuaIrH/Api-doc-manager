#!/bin/bash

# API Document Manager - 前端开发环境启动脚本
# 使用方法: ./start-dev.sh

echo "=========================================="
echo "  API Document Manager - 前端开发环境"
echo "=========================================="

# 检查是否已安装pnpm
if ! command -v pnpm &> /dev/null; then
    echo "错误: 未安装 pnpm"
    echo "请先安装 pnpm: npm install -g pnpm"
    exit 1
fi

# 检查node_modules是否存在
if [ ! -d "node_modules" ]; then
    echo ""
    echo "首次运行，正在安装依赖..."
    echo ""
    pnpm install
    if [ $? -ne 0 ]; then
        echo ""
        echo "pnpm install 失败，尝试使用 npm..."
        npm install
    fi
else
    echo "依赖已安装，跳过安装步骤"
fi

echo ""
echo "正在启动开发服务器..."
echo "访问地址: http://localhost:5173"
echo "按 Ctrl+C 停止服务器"
echo ""

# 启动开发服务器
pnpm dev
