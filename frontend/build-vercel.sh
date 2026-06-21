#!/bin/bash
# Vercel 构建脚本 - 构建 Uni-app H5
echo "=== 开始构建 Uni-app H5 ==="
cd "C:/Users/ren/WorkBuddy/2026-05-25-10-18-15/two_track_notebook/frontend" || exit 1

# 安装依赖
echo "安装依赖..."
npm install

# 构建 H5
echo "构建 H5..."
npm run build:h5

echo "=== 构建完成 ==="
echo "输出目录: frontend/dist/build/h5"
