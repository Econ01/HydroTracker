#!/usr/bin/env python3
"""
Update localisation assets and app configuration.

This script is intended to run in GitHub Actions after every merge to master.
It performs three tasks:

1. Fetches translation progress from Crowdin and generates
   docs/translation-progress.svg.
2. Updates the auto-generated sections of TRANSLATIONS.md with the latest
   progress and supported-language list.
3. Scans app/src/main/res/values-*/ for translation folders and keeps
   AppLocale.SUPPORTED_TAGS and res/xml/locales_config.xml in sync.

Required environment variables:
    CROWDIN_PROJECT_ID  numeric Crowdin project id
    CROWDIN_API_TOKEN   Crowdin personal API token
"""

import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path


import requests

PROJECT_ROOT = Path(__file__).resolve().parent.parent

APP_LOCALE_KT = PROJECT_ROOT / "app/src/main/java/com/cemcakmak/hydrotracker/utils/AppLocale.kt"
LOCALES_CONFIG_XML = PROJECT_ROOT / "app/src/main/res/xml/locales_config.xml"
TRANSLATIONS_MD = PROJECT_ROOT / "TRANSLATIONS.md"
PROGRESS_SVG = PROJECT_ROOT / "docs/translation-progress.svg"
RES_VALUES_DIR = PROJECT_ROOT / "app/src/main/res"

CROWDIN_BASE = "https://api.crowdin.com/api/v2"
PROGRESS_BAR_COLOUR = "#008080"
TRACK_COLOUR = "#e0e0e0"
TEXT_COLOUR = "#333333"
BACKGROUND_COLOUR = "#ffffff"

SVG_WIDTH = 720
LEFT_MARGIN = 20
RIGHT_MARGIN = 20
TOP_MARGIN = 40
ROW_HEIGHT = 40
BAR_X = 220
BAR_WIDTH = 380
BAR_HEIGHT = 18


def crowdin_get(token: str, path: str, params: dict | None = None) -> dict:
    headers = {"Authorization": f"Bearer {token}"}
    url = f"{CROWDIN_BASE}{path}"
    response = requests.get(url, headers=headers, params=params, timeout=30)
    response.raise_for_status()
    return response.json()


def crowdin_paginate(token: str, path: str) -> list[dict]:
    """Fetch all pages of a Crowdin list endpoint and return flattened data."""
    results: list[dict] = []
    offset = 0
    limit = 100
    while True:
        data = crowdin_get(token, path, {"offset": offset, "limit": limit})
        items = [item["data"] for item in data.get("data", [])]
        if not items:
            break
        results.extend(items)
        if len(items) < limit:
            break
        offset += limit
    return results


def fetch_crowdin_progress(project_id: str, token: str) -> list[dict]:
    path = f"/projects/{project_id}/languages/progress"
    return crowdin_paginate(token, path)


def fetch_language_names(token: str) -> dict[str, str]:
    languages = crowdin_paginate(token, "/languages")
    return {lang["id"]: lang.get("name", lang["id"]) for lang in languages}


def android_dir_to_bcp47(folder_name: str) -> str:
    """
    Convert an Android resources folder qualifier to a BCP-47 tag.

    Examples:
        values-de       -> de
        values-zh-rCN   -> zh-CN
        values-pt-rPT   -> pt-PT
    """
    if not folder_name.startswith("values-"):
        return folder_name
    qualifier = folder_name[len("values-") :]
    return qualifier.replace("-r", "-")


def discover_supported_tags() -> list[str]:
    """Return the BCP-47 tags for which the app ships translation resources."""
    tags = {"en"}
    if not RES_VALUES_DIR.exists():
        return sorted(tags)

    for item in RES_VALUES_DIR.iterdir():
        if item.is_dir() and item.name.startswith("values-"):
            strings_xml = item / "strings.xml"
            if strings_xml.exists():
                tags.add(android_dir_to_bcp47(item.name))

    return sorted(tags)


def update_app_locale(supported_tags: list[str]) -> None:
    content = APP_LOCALE_KT.read_text(encoding="utf-8")
    tags_literal = ", ".join(f'"{tag}"' for tag in supported_tags)
    new_content = re.sub(
        r'val SUPPORTED_TAGS: List<String> = listOf\([^)]*\)',
        f'val SUPPORTED_TAGS: List<String> = listOf({tags_literal})',
        content,
    )
    if new_content == content:
        print("AppLocale.kt is already up to date.")
        return
    APP_LOCALE_KT.write_text(new_content, encoding="utf-8")
    print("Updated AppLocale.kt.")


def update_locales_config(supported_tags: list[str]) -> None:
    if not LOCALES_CONFIG_XML.exists():
        print(f"Warning: {LOCALES_CONFIG_XML} not found.", file=sys.stderr)
        return

    content = LOCALES_CONFIG_XML.read_text(encoding="utf-8")
    locale_lines = [f'    <locale android:name="{tag}" />' for tag in supported_tags]
    new_block = "\n".join(locale_lines)

    # Replace everything between the opening and closing locale-config tags,
    # preserving the tag itself, comments and namespace declarations.
    new_content = re.sub(
        r"(<locale-config[^>]*>\n)(.*?)(\n</locale-config>)",
        lambda m: m.group(1) + new_block + m.group(3),
        content,
        flags=re.DOTALL,
    )

    if new_content == content:
        print("locales_config.xml is already up to date.")
        return

    LOCALES_CONFIG_XML.write_text(new_content, encoding="utf-8")
    print("Updated locales_config.xml.")


def generate_progress_svg(progress: list[dict], language_names: dict[str, str]) -> str:
    rows = sorted(progress, key=lambda item: language_names.get(item["languageId"], item["languageId"]))
    row_count = len(rows)
    svg_height = TOP_MARGIN + row_count * ROW_HEIGHT + 20

    svg = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{SVG_WIDTH}" height="{svg_height}" role="img" aria-label="Translation progress">',
        f'    <rect width="{SVG_WIDTH}" height="{svg_height}" fill="{BACKGROUND_COLOUR}"/>',
        f'    <text x="{LEFT_MARGIN}" y="28" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, sans-serif" font-size="18" font-weight="bold" fill="{TEXT_COLOUR}">Translation Progress</text>',
    ]

    for index, row in enumerate(rows):
        lang_id = row["languageId"]
        lang_name = language_names.get(lang_id, lang_id)
        percent = row.get("translationProgress", 0)
        fill_width = int(BAR_WIDTH * percent / 100)

        y = TOP_MARGIN + index * ROW_HEIGHT
        text_y = y + ROW_HEIGHT // 2 + 5

        svg.append(f'    <text x="{LEFT_MARGIN}" y="{text_y}" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, sans-serif" font-size="14" fill="{TEXT_COLOUR}">{lang_name}</text>')
        svg.append(f'    <rect x="{BAR_X}" y="{y + (ROW_HEIGHT - BAR_HEIGHT) // 2}" width="{BAR_WIDTH}" height="{BAR_HEIGHT}" rx="4" fill="{TRACK_COLOUR}"/>')
        if fill_width > 0:
            svg.append(f'    <rect x="{BAR_X}" y="{y + (ROW_HEIGHT - BAR_HEIGHT) // 2}" width="{fill_width}" height="{BAR_HEIGHT}" rx="4" fill="{PROGRESS_BAR_COLOUR}"/>')
        svg.append(f'    <text x="{BAR_X + BAR_WIDTH + 12}" y="{text_y}" font-family="-apple-system, BlinkMacSystemFont, Segoe UI, sans-serif" font-size="14" fill="{TEXT_COLOUR}">{percent}%</text>')

    svg.append("</svg>")
    return "\n".join(svg) + "\n"


def update_translations_md(progress: list[dict], language_names: dict[str, str], supported_tags: list[str]) -> None:
    if not TRANSLATIONS_MD.exists():
        print(f"Warning: {TRANSLATIONS_MD} not found.", file=sys.stderr)
        return

    content = TRANSLATIONS_MD.read_text(encoding="utf-8")
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    # Progress section
    progress_block = (
        "<!-- TRANSLATION_PROGRESS_START -->\n"
        f"*Last updated: {timestamp}*\n\n"
        f'<img src="docs/translation-progress.svg" alt="Translation progress" width="{SVG_WIDTH}">\n'
        "<!-- TRANSLATION_PROGRESS_END -->"
    )
    content = re.sub(
        r"<!-- TRANSLATION_PROGRESS_START -->.*?<!-- TRANSLATION_PROGRESS_END -->",
        progress_block,
        content,
        flags=re.DOTALL,
    )

    # Supported languages section
    language_lines = ["- English (source)"]
    for tag in supported_tags:
        if tag == "en":
            continue
        # Use Crowdin name if available; otherwise Android display name fallback is not
        # available here, so use the tag itself.
        name = language_names.get(tag, tag)
        language_lines.append(f"- {name}")

    supported_block = (
        "<!-- SUPPORTED_LANGUAGES_START -->\n"
        "HydroTracker currently ships with English as the source language. The following target languages are active on Crowdin:\n\n"
        + "\n".join(language_lines)
        + "\n<!-- SUPPORTED_LANGUAGES_END -->"
    )
    content = re.sub(
        r"<!-- SUPPORTED_LANGUAGES_START -->.*?<!-- SUPPORTED_LANGUAGES_END -->",
        supported_block,
        content,
        flags=re.DOTALL,
    )

    TRANSLATIONS_MD.write_text(content, encoding="utf-8")
    print("Updated TRANSLATIONS.md.")


def main() -> int:
    project_id = os.environ.get("CROWDIN_PROJECT_ID")
    token = os.environ.get("CROWDIN_API_TOKEN")

    if not project_id or not token:
        print(
            "Error: CROWDIN_PROJECT_ID and CROWDIN_API_TOKEN must be set.",
            file=sys.stderr,
        )
        return 1

    print("Fetching Crowdin translation progress...")
    progress = fetch_crowdin_progress(project_id, token)
    print(f"Found progress for {len(progress)} language(s).")

    print("Fetching Crowdin language names...")
    language_names = fetch_language_names(token)

    print("Discovering supported Android locales...")
    supported_tags = discover_supported_tags()
    print(f"Supported tags: {supported_tags}")

    print("Generating progress SVG...")
    PROGRESS_SVG.parent.mkdir(parents=True, exist_ok=True)
    svg_content = generate_progress_svg(progress, language_names)
    PROGRESS_SVG.write_text(svg_content, encoding="utf-8")

    print("Updating TRANSLATIONS.md...")
    update_translations_md(progress, language_names, supported_tags)

    print("Updating app locale configuration...")
    update_app_locale(supported_tags)
    update_locales_config(supported_tags)

    print("Done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
