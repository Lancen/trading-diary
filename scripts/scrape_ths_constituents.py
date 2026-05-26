#!/usr/bin/env python3
"""
从同花顺抓取行业和概念板块的成分股列表。

用法:
    # 全量抓取
    python3 scripts/scrape_ths_constituents.py              # 抓取全部（行业+概念）
    python3 scripts/scrape_ths_constituents.py --concept    # 仅概念
    python3 scripts/scrape_ths_constituents.py --industry   # 仅行业
    python3 scripts/scrape_ths_constituents.py --output data.json

    # 单板块抓取（供管理后台调用）
    python3 scripts/scrape_ths_constituents.py --type industry --code 881121
    python3 scripts/scrape_ths_constituents.py --type concept --code 308614

输出 JSON 格式:
{
  "fetched_at": "2026-05-17T15:00:00",
  "fetched_date": "2026-05-17",
  "source": "同花顺 (10jqka.com.cn)",
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
    print("请先安装 playwright: pip3 install playwright && playwright install chromium", flush=True)
    sys.exit(1)

BASE = "https://q.10jqka.com.cn"
CONCEPT_LIST = f"{BASE}/gn/"
INDUSTRY_LIST = f"{BASE}/thshy/"


async def extract_board_links(page, list_url, detail_path):
    links = await page.evaluate(f"""
        () => {{
            const links = document.querySelectorAll('a[href*="/{detail_path}/detail/code/"]');
            return Array.from(links).map(l => ({{
                name: l.textContent.trim(),
                code: l.href.match(/code\\/(\\d+)/)?.[1] || ''
            }}));
        }}
    """)

    seen = set()
    unique = []
    for item in links:
        if item["code"] and item["code"] not in seen:
            seen.add(item["code"])
            unique.append(item)

    return unique


async def scrape_board_stocks(page, board, detail_path):
    code = board["code"]
    all_stocks = []

    try:
        detail_url = f"{BASE}/{detail_path}/detail/code/{code}/"
        await page.goto(detail_url, wait_until="domcontentloaded")
        await page.wait_for_timeout(2000)

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

        for page_num in range(1, total_pages + 1):
            if page_num > 1:
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
        print(f"ERROR: {board['name']}({code}) 抓取出错: {e}", file=sys.stderr, flush=True)

    return all_stocks


async def scrape_single(board_type, code):
    detail_path = "thshy" if board_type == "industry" else "gn"

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        context = await browser.new_context(
            viewport={"width": 1400, "height": 900},
            user_agent="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                       "(KHTML, like Gecko) Chrome/147.0.0.0 Safari/537.36",
        )
        page = await context.new_page()

        try:
            board = {"code": code, "name": code}
            stocks = await scrape_board_stocks(page, board, detail_path)
        finally:
            await browser.close()

    result = {
        "fetched_at": datetime.now(timezone.utc).isoformat(),
        "fetched_date": date.today().isoformat(),
        "source": "同花顺 (10jqka.com.cn)",
        "industries": [],
        "concepts": [],
    }

    key = "industries" if board_type == "industry" else "concepts"
    result[key].append({"code": code, "name": "", "stocks": stocks})

    return result


async def scrape_batch(do_industry, do_concept, limit, output_path):
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
            if do_industry:
                print("抓取行业板块...", flush=True)
                await page.goto(INDUSTRY_LIST, wait_until="domcontentloaded")
                await page.wait_for_timeout(3000)
                industries = await extract_board_links(page, INDUSTRY_LIST, "thshy")
                if limit > 0:
                    industries = industries[:limit]

                for i, ind in enumerate(industries):
                    print(f"  [{i+1}/{len(industries)}] {ind['name']} ({ind['code']})", flush=True)
                    stocks = await scrape_board_stocks(page, ind, "thshy")
                    result["industries"].append({
                        "code": ind["code"],
                        "name": ind["name"],
                        "stocks": stocks,
                    })
                    print(f"    -> {len(stocks)} 只成分股", flush=True)
                    await page.wait_for_timeout(500)

            if do_concept:
                print("抓取概念板块...", flush=True)
                await page.goto(CONCEPT_LIST, wait_until="domcontentloaded")
                await page.wait_for_timeout(3000)
                concepts = await extract_board_links(page, CONCEPT_LIST, "gn")
                if limit > 0:
                    concepts = concepts[:limit]

                for i, con in enumerate(concepts):
                    print(f"  [{i+1}/{len(concepts)}] {con['name']} ({con['code']})", flush=True)
                    stocks = await scrape_board_stocks(page, con, "gn")
                    result["concepts"].append({
                        "code": con["code"],
                        "name": con["name"],
                        "stocks": stocks,
                    })
                    print(f"    -> {len(stocks)} 只成分股", flush=True)
                    await page.wait_for_timeout(500)

        finally:
            await browser.close()

    ind_total = sum(len(x["stocks"]) for x in result["industries"])
    con_total = sum(len(x["stocks"]) for x in result["concepts"])
    print(f"\n完成: {len(result['industries'])} 个行业({ind_total} 条), "
          f"{len(result['concepts'])} 个概念({con_total} 条)", flush=True)

    today = date.today().isoformat()
    out_dir = "data/constituents"
    out_file = output_path or os.path.join(out_dir, f"constituents-{today}.json")
    os.makedirs(os.path.dirname(out_file), exist_ok=True)

    output = json.dumps(result, ensure_ascii=False, indent=2)
    with open(out_file, "w", encoding="utf-8") as f:
        f.write(output)

    print(f"已保存到 {out_file}", flush=True)


def main():
    parser = argparse.ArgumentParser(description="抓取同花顺行业/概念成分股")
    parser.add_argument("--concept", action="store_true", help="仅抓取概念板块")
    parser.add_argument("--industry", action="store_true", help="仅抓取行业板块")
    parser.add_argument("--output", "-o", default=None, help="输出文件路径")
    parser.add_argument("--limit", type=int, default=0, help="限制抓取板块数（调试用）")
    parser.add_argument("--type", dest="board_type", choices=["industry", "concept"],
                        help="单板块抓取时的板块类型")
    parser.add_argument("--code", help="单板块抓取时的板块代码")
    args = parser.parse_args()

    if args.board_type and args.code:
        result = asyncio.run(scrape_single(args.board_type, args.code))
        print(json.dumps(result, ensure_ascii=False), flush=True)
    else:
        do_concept = args.concept or (not args.industry)
        do_industry = args.industry or (not args.concept)
        asyncio.run(scrape_batch(do_industry, do_concept, args.limit, args.output))


if __name__ == "__main__":
    main()
