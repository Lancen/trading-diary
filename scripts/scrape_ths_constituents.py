#!/usr/bin/env python3
"""
从同花顺抓取行业和概念板块的成分股列表。

用法:
    python3 scripts/scrape_ths_constituents.py              # 抓取全部（行业+概念）
    python3 scripts/scrape_ths_constituents.py --concept    # 仅概念
    python3 scripts/scrape_ths_constituents.py --industry   # 仅行业
    python3 scripts/scrape_ths_constituents.py --output data.json

输出 JSON 格式:
{
  "fetched_at": "2026-05-17T15:00:00",
  "industries": [
    {"code": "881121", "name": "半导体", "stocks": ["000001", "000002", ...]},
    ...
  ],
  "concepts": [
    {"code": "308614", "name": "阿尔茨海默概念", "stocks": ["002693", ...]},
    ...
  ]
}

依赖: pip3 install playwright && playwright install chromium
"""

import argparse
import asyncio
import json
import os
import sys
from datetime import date, datetime, timezone

try:
    from playwright.async_api import async_playwright
except ImportError:
    print("请先安装 playwright: pip3 install playwright && playwright install chromium")
    sys.exit(1)

BASE = "https://q.10jqka.com.cn"
CONCEPT_LIST = f"{BASE}/gn/"
INDUSTRY_LIST = f"{BASE}/thshy/"


async def extract_board_links(page, list_url, detail_path):
    """从列表页提取板块链接（名称+代码）"""
    print(f"  打开列表页: {list_url}")
    await page.goto(list_url, wait_until="domcontentloaded")
    await page.wait_for_timeout(3000)

    links = await page.evaluate(f"""
        () => {{
            const links = document.querySelectorAll('a[href*="/{detail_path}/detail/code/"]');
            return Array.from(links).map(l => ({{
                name: l.textContent.trim(),
                code: l.href.match(/code\\/(\\d+)/)?.[1] || ''
            }}));
        }}
    """)

    # 去重（某些链接可能重复）
    seen = set()
    unique = []
    for item in links:
        if item["code"] and item["code"] not in seen:
            seen.add(item["code"])
            unique.append(item)

    print(f"  找到 {len(unique)} 个板块")
    return unique


async def scrape_board_stocks(page, board, detail_path):
    """抓取单个板块的全部成分股（通过分页 URL 直接访问）"""
    code = board["code"]
    all_stocks = []

    try:
        # 从列表页跳转到详情页（建立 Referer 链）
        detail_url = f"{BASE}/{detail_path}/detail/code/{code}/"
        await page.goto(detail_url, wait_until="domcontentloaded")
        await page.wait_for_timeout(2000)

        # 获取总页数
        total_pages = await page.evaluate("""
            () => {
                const pager = document.querySelector('.m-pager');
                if (!pager) return 1;
                const info = pager.querySelector('.page_info');
                if (info) {
                    const parts = info.textContent.trim().split('/');
                    return parts.length === 2 ? parseInt(parts[1]) : 1;
                }
                return 1;
            }
        """)

        # 逐页抓取
        for page_num in range(1, total_pages + 1):
            if page_num > 1:
                # 直接访问分页 URL
                page_url = f"{BASE}/{detail_path}/detail/field/stock/order/asc/page/{page_num}/ajax/1/code/{code}/"
                await page.goto(page_url, wait_until="domcontentloaded")
                await page.wait_for_timeout(1000)

            page_stocks = await page.evaluate("""
                () => {
                    const rows = document.querySelectorAll('.m-table tbody tr');
                    const stocks = [];
                    for (const row of rows) {
                        const cells = row.querySelectorAll('td');
                        if (cells.length >= 2) {
                            const code = cells[1]?.textContent?.trim();
                            if (code && /^\\d{6}$/.test(code)) {
                                stocks.push(code);
                            }
                        }
                    }
                    return stocks;
                }
            """)
            all_stocks.extend(page_stocks)

    except Exception as e:
        print(f"    ⚠️ {board['name']}({code}) 抓取出错: {e}")

    return all_stocks


async def main():
    parser = argparse.ArgumentParser(description="抓取同花顺行业/概念成分股")
    parser.add_argument("--concept", action="store_true", help="仅抓取概念板块")
    parser.add_argument("--industry", action="store_true", help="仅抓取行业板块")
    parser.add_argument("--output", "-o", default=None, help="输出文件路径（默认 stdout）")
    parser.add_argument("--limit", type=int, default=0, help="限制抓取板块数（调试用）")
    args = parser.parse_args()

    do_concept = args.concept or (not args.industry)
    do_industry = args.industry or (not args.concept)

    result = {
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "fetched_date": date.today().isoformat(),
        "source": "同花顺 (10jqka.com.cn)",
        "industries": [],
        "concepts": [],
    }

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1400, "height": 900},
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                       "(KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
        )
        page = await context.new_page()

        try:
            # 行业板块
            if do_industry:
                print("📊 抓取行业板块...")
                industries = await extract_board_links(page, INDUSTRY_LIST, "thshy")
                if args.limit > 0:
                    industries = industries[:args.limit]

                for i, ind in enumerate(industries):
                    print(f"  [{i+1}/{len(industries)}] {ind['name']} ({ind['code']})")
                    stocks = await scrape_board_stocks(page, ind, "thshy")
                    result["industries"].append({
                        "code": ind["code"],
                        "name": ind["name"],
                        "stocks": stocks,
                    })
                    print(f"    -> {len(stocks)} 只成分股")
                    await page.wait_for_timeout(500)

            # 概念板块
            if do_concept:
                print("📊 抓取概念板块...")
                concepts = await extract_board_links(page, CONCEPT_LIST, "gn")
                if args.limit > 0:
                    concepts = concepts[:args.limit]

                for i, con in enumerate(concepts):
                    print(f"  [{i+1}/{len(concepts)}] {con['name']} ({con['code']})")
                    stocks = await scrape_board_stocks(page, con, "gn")
                    result["concepts"].append({
                        "code": con["code"],
                        "name": con["name"],
                        "stocks": stocks,
                    })
                    print(f"    -> {len(stocks)} 只成分股")
                    await page.wait_for_timeout(500)

        finally:
            await browser.close()

    # 统计
    ind_total = sum(len(x["stocks"]) for x in result["industries"])
    con_total = sum(len(x["stocks"]) for x in result["concepts"])
    print(f"\n✅ 完成: {len(result['industries'])} 个行业({ind_total} 条), "
          f"{len(result['concepts'])} 个概念({con_total} 条)")

    # 输出（默认保存到 data/constituents/ 目录，文件名含日期）
    today = date.today().isoformat()
    out_dir = "data/constituents"
    out_file = args.output or os.path.join(out_dir, f"constituents-{today}.json")
    os.makedirs(os.path.dirname(out_file), exist_ok=True)

    output = json.dumps(result, ensure_ascii=False, indent=2)
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(output)

    print(f"已保存到 {out_file}")
    print(f"  行业: {len(result['industries'])} 个板块, {ind_total} 条成分股关系")
    print(f"  概念: {len(result['concepts'])} 个板块, {con_total} 条成分股关系")


if __name__ == "__main__":
    asyncio.run(main())
