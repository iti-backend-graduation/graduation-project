#!/usr/bin/env python3
"""Render Markdown corpus sources with Chromium and report physical PDF pages."""

from __future__ import annotations

import argparse
import html
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]
SOURCES = (ROOT / "docs", ROOT / "docs-src")
PAGE_OBJECT = re.compile(rb"/Type\s*/Page\b")
WORD = re.compile(r"\b[\w]+(?:['’][\w]+)?\b", re.UNICODE)
TABLE_SEPARATOR = re.compile(r"^\s*\|?[\s:|-]+\|?\s*$")


def browser_path() -> str:
    configured = os.environ.get("CHROME_BIN")
    candidates = [
        configured,
        shutil.which("chrome"),
        shutil.which("chromium"),
        shutil.which("chromium-browser"),
        shutil.which("google-chrome"),
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    ]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError("Install Chrome/Chromium or set CHROME_BIN to its executable path.")


def inline_markup(text: str) -> str:
    escaped = html.escape(text, quote=False)
    escaped = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", escaped)
    escaped = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", escaped)
    escaped = re.sub(r"`([^`]+)`", r"<code>\1</code>", escaped)
    return escaped


def table_cells(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def markdown_html(text: str) -> str:
    lines = text.splitlines()
    blocks: list[str] = []
    index = 0
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            index += 1
            continue
        if line.startswith("```"):
            code: list[str] = []
            index += 1
            while index < len(lines) and not lines[index].startswith("```"):
                code.append(lines[index])
                index += 1
            blocks.append(f"<pre><code>{html.escape(chr(10).join(code))}</code></pre>")
            index += 1
            continue
        heading = re.match(r"^(#{1,6})\s+(.+)$", line)
        if heading:
            level = len(heading.group(1))
            blocks.append(f"<h{level}>{inline_markup(heading.group(2))}</h{level}>")
            index += 1
            continue
        if "|" in line and index + 1 < len(lines) and TABLE_SEPARATOR.match(lines[index + 1]):
            rows = [table_cells(line)]
            index += 2
            while index < len(lines) and "|" in lines[index] and lines[index].strip():
                rows.append(table_cells(lines[index]))
                index += 1
            header = "".join(f"<th>{inline_markup(cell)}</th>" for cell in rows[0])
            body = "".join(
                "<tr>" + "".join(f"<td>{inline_markup(cell)}</td>" for cell in row) + "</tr>"
                for row in rows[1:]
            )
            blocks.append(f"<table><thead><tr>{header}</tr></thead><tbody>{body}</tbody></table>")
            continue
        if line.lstrip().startswith(">"):
            quote: list[str] = []
            while index < len(lines) and lines[index].lstrip().startswith(">"):
                quote.append(lines[index].lstrip()[1:].strip())
                index += 1
            blocks.append(f"<blockquote>{inline_markup(' '.join(quote))}</blockquote>")
            continue
        if re.match(r"^\s*(?:[-*+]|\d+[.)])\s+", line):
            ordered = bool(re.match(r"^\s*\d+[.)]\s+", line))
            tag = "ol" if ordered else "ul"
            items: list[str] = []
            while index < len(lines):
                item = re.match(r"^\s*(?:[-*+]|\d+[.)])\s+(.+)$", lines[index])
                if not item:
                    break
                items.append(f"<li>{inline_markup(item.group(1))}</li>")
                index += 1
            blocks.append(f"<{tag}>{''.join(items)}</{tag}>")
            continue

        paragraph = [line.strip()]
        index += 1
        while index < len(lines) and lines[index].strip():
            if (re.match(r"^(#{1,6})\s+", lines[index])
                    or re.match(r"^\s*(?:[-*+]|\d+[.)])\s+", lines[index])
                    or lines[index].lstrip().startswith(">")):
                break
            paragraph.append(lines[index].strip())
            index += 1
        blocks.append(f"<p>{inline_markup(' '.join(paragraph))}</p>")
    return "\n".join(blocks)


def corpus_html(files: list[Path]) -> tuple[str, int]:
    documents: list[str] = []
    word_count = 0
    for index, file in enumerate(files):
        text = file.read_text(encoding="utf-8")
        word_count += len(WORD.findall(text))
        page_class = "source first-source" if index == 0 else "source"
        documents.append(
            f'<article class="{page_class}"><div class="source-name">{html.escape(file.name)}</div>'
            f"{markdown_html(text)}</article>"
        )
    markup = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>Synthetic Markdown Corpus</title>
<style>
@page {{ size: letter; margin: 0.55in 1.25in; }}
* {{ box-sizing: border-box; }}
body {{ font-family: Arial, Helvetica, sans-serif; font-size: 13pt; line-height: 1.75; color: #111; }}
.source {{ break-before: page; page-break-before: always; }}
.source-name {{ font-size: 9pt; color: #555; border-bottom: 1px solid #999; margin-bottom: 0.5em; }}
h1 {{ font-size: 21pt; line-height: 1.25; margin: 0.6em 0 0.35em; break-after: avoid; }}
h2 {{ font-size: 17pt; line-height: 1.25; margin: 0.75em 0 0.3em; break-after: avoid; }}
h3,h4,h5,h6 {{ font-size: 14pt; line-height: 1.3; margin: 0.65em 0 0.25em; break-after: avoid; }}
p {{ margin: 0 0 0.55em; orphans: 3; widows: 3; }}
ul,ol {{ padding-left: 1.5em; margin: 0.2em 0 0.6em; }}
li {{ padding-left: 0.1em; margin: 0 0 0.25em; }}
table {{ width: 100%; border-collapse: collapse; font-size: 10pt; line-height: 1.4; margin: 0.6em 0; }}
th,td {{ border: 1px solid #777; padding: 0.25em 0.35em; text-align: left; vertical-align: top; }}
blockquote {{ border-left: 3px solid #777; padding-left: 0.7em; margin-left: 0; color: #333; }}
code {{ font-family: Consolas, monospace; font-size: 0.9em; }}
pre {{ white-space: pre-wrap; overflow-wrap: anywhere; }}
</style></head><body>{''.join(documents)}</body></html>"""
    return markup, word_count


def pdf_pages(path: Path) -> int:
    data = path.read_bytes()
    count = len(PAGE_OBJECT.findall(data))
    if count == 0:
        raise RuntimeError(f"Could not read page objects from PDF: {path}")
    return count


def render(browser: str, markup: str, output: Path) -> str:
    version = subprocess.run(
        [browser, "--version"], check=True, capture_output=True, text=True, timeout=15
    ).stdout.strip()
    with tempfile.TemporaryDirectory(prefix="markdown-corpus-print-") as directory:
        temp = Path(directory)
        source = temp / "corpus.html"
        rendered = temp / "corpus.pdf"
        profile = temp / "chrome-profile"
        source.write_text(markup, encoding="utf-8")
        subprocess.run(
            [
                browser,
                "--headless=new",
                "--disable-gpu",
                "--no-pdf-header-footer",
                f"--user-data-dir={profile}",
                f"--print-to-pdf={rendered}",
                source.resolve().as_uri(),
            ],
            check=True,
            capture_output=True,
            text=True,
            timeout=90,
        )
        if not rendered.is_file():
            raise RuntimeError("Chrome completed without producing the Markdown corpus PDF.")
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(rendered, output)
    return version


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output", type=Path, default=ROOT / "target" / "markdown-corpus-print.pdf"
    )
    args = parser.parse_args()
    files = sorted(path for directory in SOURCES for path in directory.glob("*.md"))
    if not files:
        raise RuntimeError("No Markdown corpus sources found.")

    markup, words = corpus_html(files)
    browser = browser_path()
    version = render(browser, markup, args.output.resolve())
    markdown_pages = pdf_pages(args.output.resolve())
    source_pdf_files = sorted(path for directory in SOURCES for path in directory.glob("*.pdf"))
    source_pdf_pages = sum(pdf_pages(path) for path in source_pdf_files)
    total_pages = markdown_pages + source_pdf_pages

    print(f"Renderer: {version}")
    print(f"Markdown documents: {len(files)}")
    print(f"Markdown words: {words}")
    print(f"Rendered Markdown PDF: {args.output.resolve()}")
    print(f"Rendered Markdown physical pages: {markdown_pages}")
    print(f"Source PDF documents: {len(source_pdf_files)}")
    print(f"Source PDF physical pages: {source_pdf_pages}")
    print(f"Total corpus documents: {len(files) + len(source_pdf_files)}")
    print(f"Total corpus physical pages: {total_pages}")
    print(f"Target 60-80 pages met: {60 <= total_pages <= 80}")
    return 0 if 60 <= total_pages <= 80 else 2


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"Corpus page count failed: {error}", file=sys.stderr)
        raise SystemExit(1)
