#!/usr/bin/env python3
"""
Filter Meteor Client addons based on quality and version compatibility.

Usage:
    # Find verified addons for latest Minecraft version
    python filter_addons.py --verified
    
    # Find addons for specific Minecraft version
    python filter_addons.py --mc-version 1.21.1 --verified
    
    # Search for specific features
    python filter_addons.py --feature-type modules --feature-name ExampleModule
    
    # Include archived addons (normally excluded)
    python filter_addons.py --include-archived
"""

import argparse
import json
import sys
from urllib.request import urlopen
from typing import List, Dict, Any

ADDONS_DB_URL = "https://raw.githubusercontent.com/cqb13/meteor-addon-scanner/refs/heads/addons/addons.json"


def fetch_addons() -> List[Dict[str, Any]]:
    """Fetch the addons database from GitHub."""
    try:
        with urlopen(ADDONS_DB_URL) as response:
            return json.loads(response.read().decode())
    except Exception as e:
        print(f"Error fetching addons database: {e}", file=sys.stderr)
        sys.exit(1)


def filter_addons(
    addons: List[Dict[str, Any]],
    mc_version: str = None,
    verified_only: bool = True,
    include_archived: bool = False,
    feature_type: str = None,
    feature_name: str = None,
    min_stars: int = 0,
    sort_by: str = "stars"
) -> List[Dict[str, Any]]:
    """
    Filter addons based on various criteria.
    
    Args:
        addons: List of addon dictionaries
        mc_version: Filter by Minecraft version (e.g., "1.21.1")
        verified_only: Only include verified addons
        include_archived: Include archived repositories
        feature_type: Filter by feature type (modules, commands, hud_elements, custom_screens)
        feature_name: Filter by specific feature name
        min_stars: Minimum GitHub stars
        sort_by: Sort results by field (stars, downloads, last_update)
    
    Returns:
        Filtered and sorted list of addons
    """
    filtered = []
    
    for addon in addons:
        # Skip archived unless explicitly included
        if not include_archived and addon.get("repo", {}).get("archived", False):
            continue
        
        # Verify status
        if verified_only and not addon.get("verified", False):
            continue
        
        # Minecraft version match
        if mc_version:
            addon_mc_version = addon.get("mc_version", "")
            supported_versions = addon.get("custom", {}).get("supported_versions") or []
            
            if mc_version not in [addon_mc_version] + supported_versions:
                continue
        
        # Feature filtering
        if feature_type and feature_name:
            features = addon.get("features", {}).get(feature_type) or []
            if feature_name not in features:
                continue
        
        # Star count
        stars = addon.get("repo", {}).get("stars", 0)
        if stars < min_stars:
            continue
        
        filtered.append(addon)
    
    # Sort results
    if sort_by == "stars":
        filtered.sort(key=lambda x: x.get("repo", {}).get("stars", 0), reverse=True)
    elif sort_by == "downloads":
        filtered.sort(key=lambda x: x.get("repo", {}).get("downloads", 0), reverse=True)
    elif sort_by == "last_update":
        filtered.sort(key=lambda x: x.get("repo", {}).get("last_update", ""), reverse=True)
    
    return filtered


def format_addon_summary(addon: Dict[str, Any]) -> str:
    """Format an addon into a readable summary."""
    name = addon.get("name", "Unknown")
    desc = addon.get("description", "No description")
    mc_version = addon.get("mc_version", "Unknown")
    verified = "[VERIFIED]" if addon.get("verified", False) else "[UNVERIFIED]"
    
    repo = addon.get("repo", {})
    stars = repo.get("stars", 0)
    owner = repo.get("owner", "Unknown")
    repo_name = repo.get("name", "Unknown")
    github_url = addon.get("links", {}).get("github", "")
    
    features = addon.get("features", {})
    module_count = len(features.get("modules", []) or [])
    command_count = len(features.get("commands", []) or [])
    hud_count = len(features.get("hud_elements", []) or [])
    
    return f"""
{name} {verified}
  MC Version: {mc_version}
  Repository: {owner}/{repo_name}
  Stars: {stars}
  URL: {github_url}
  Description: {desc}
  Features: {module_count} modules, {command_count} commands, {hud_count} HUD elements
"""


def main():
    parser = argparse.ArgumentParser(
        description="Filter Meteor Client addons by quality and version"
    )
    parser.add_argument(
        "--mc-version",
        help="Filter by Minecraft version (e.g., 1.21.1)"
    )
    parser.add_argument(
        "--verified",
        action="store_true",
        help="Only show verified addons (default: True unless --no-verified)"
    )
    parser.add_argument(
        "--no-verified",
        action="store_true",
        help="Include non-verified addons"
    )
    parser.add_argument(
        "--include-archived",
        action="store_true",
        help="Include archived repositories"
    )
    parser.add_argument(
        "--feature-type",
        choices=["modules", "commands", "hud_elements", "custom_screens"],
        help="Filter by feature type"
    )
    parser.add_argument(
        "--feature-name",
        help="Filter by specific feature name (requires --feature-type)"
    )
    parser.add_argument(
        "--min-stars",
        type=int,
        default=0,
        help="Minimum GitHub stars"
    )
    parser.add_argument(
        "--sort-by",
        choices=["stars", "downloads", "last_update"],
        default="stars",
        help="Sort results by field"
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Output raw JSON instead of formatted text"
    )
    parser.add_argument(
        "--limit",
        type=int,
        help="Limit number of results"
    )
    
    args = parser.parse_args()
    
    # Fetch addons
    print("Fetching addons database...", file=sys.stderr)
    addons = fetch_addons()
    print(f"Found {len(addons)} total addons", file=sys.stderr)
    
    # Filter
    verified_only = args.verified or not args.no_verified
    filtered = filter_addons(
        addons,
        mc_version=args.mc_version,
        verified_only=verified_only,
        include_archived=args.include_archived,
        feature_type=args.feature_type,
        feature_name=args.feature_name,
        min_stars=args.min_stars,
        sort_by=args.sort_by
    )
    
    if args.limit:
        filtered = filtered[:args.limit]
    
    print(f"Filtered to {len(filtered)} addons\n", file=sys.stderr)
    
    # Output
    if args.json:
        print(json.dumps(filtered, indent=2))
    else:
        for addon in filtered:
            print(format_addon_summary(addon))


if __name__ == "__main__":
    main()
