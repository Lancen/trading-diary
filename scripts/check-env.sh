#!/bin/bash
set -e

echo "=== 环境检查 ==="

# Java 版本（要求 17+）
echo -n "[java] "
JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oE '[0-9]+' | head -1 || echo "0")
if [ "$JAVA_VERSION" -ge 17 ]; then
    echo "✅ JDK $JAVA_VERSION"
else
    echo "❌ 需要 JDK 17+，当前 JDK $JAVA_VERSION"
    echo "   提示: export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
    exit 1
fi

# 后端端口 8080
echo -n "[port:8080] "
if lsof -ti:8080 >/dev/null 2>&1; then
    echo "⚠️  已占用（可能是后端运行中）"
else
    echo "✅ 空闲"
fi

# AKTools（按需）
echo -n "[AKTools:8081] "
HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/version 2>/dev/null || echo "000")
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ 运行中"
else
    echo "⚠️  未运行（不需要 AKTools 的 Phase 可忽略）"
fi

# 前端端口 3000
echo -n "[port:3000] "
if lsof -ti:3000 >/dev/null 2>&1; then
    echo "⚠️  已占用（可能是前端运行中）"
else
    echo "✅ 空闲"
fi

# 编译
echo -n "[compile] "
JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || echo '')}"
if [ -z "$JAVA_HOME" ]; then
    echo "❌ JAVA_HOME 未设置，且找不到 JDK 17"
    exit 1
fi
if "$JAVA_HOME/bin/java" -version >/dev/null 2>&1; then
    if JAVA_HOME="$JAVA_HOME" ./gradlew compileJava --quiet 2>/dev/null; then
        echo "✅ BUILD SUCCESSFUL"
    else
        echo "❌ 编译失败"
        exit 1
    fi
else
    echo "❌ JAVA_HOME=$JAVA_HOME 无效"
    exit 1
fi

# .env 文件
echo -n "[.env] "
if [ -f .env ]; then
    echo "✅ 存在"
else
    echo "❌ 缺少 .env 文件"
    exit 1
fi

echo ""
echo "=== 环境检查通过 ==="
