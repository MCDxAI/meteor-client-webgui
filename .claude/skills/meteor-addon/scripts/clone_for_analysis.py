#!/usr/bin/env python3
"""
Clone Meteor Client addon repositories for analysis.

This script helps clone addon repositories into a structured reference directory
for code analysis. It handles cleanup and organization automatically.

Usage:
    # Clone a single addon
    python clone_for_analysis.py https://github.com/owner/repo
    
    # Clone multiple addons from filter results
    python filter_addons.py --mc-version 1.21.1 --json | \
        python clone_for_analysis.py --from-json
    
    # Clone to specific directory
    python clone_for_analysis.py https://github.com/owner/repo --target ./my_refs
    
    # Clean up all cloned repos
    python clone_for_analysis.py --cleanup
"""

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import List

DEFAULT_TARGET = "./ai_reference"


def clone_repo(repo_url: str, target_dir: Path) -> bool:
    """
    Clone a repository into the target directory.
    
    Args:
        repo_url: GitHub repository URL
        target_dir: Directory to clone into
    
    Returns:
        True if successful, False otherwise
    """
    # Extract repo name from URL
    repo_name = repo_url.rstrip('/').split('/')[-1]
    if repo_name.endswith('.git'):
        repo_name = repo_name[:-4]
    
    clone_path = target_dir / repo_name
    
    # Skip if already exists
    if clone_path.exists():
        print(f"[SKIP] {repo_name} (already exists)", file=sys.stderr)
        return True
    
    print(f"[CLONE] Cloning {repo_name}...", file=sys.stderr)
    
    try:
        result = subprocess.run(
            ['git', 'clone', '--depth=1', repo_url, str(clone_path)],
            capture_output=True,
            text=True,
            check=True
        )
        print(f"[SUCCESS] Cloned {repo_name}", file=sys.stderr)
        return True
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] Failed to clone {repo_name}: {e.stderr}", file=sys.stderr)
        return False


def cleanup_references(target_dir: Path) -> None:
    """Remove all cloned repositories."""
    if not target_dir.exists():
        print(f"No reference directory found at {target_dir}", file=sys.stderr)
        return
    
    import shutil
    print(f"[CLEANUP] Cleaning up {target_dir}...", file=sys.stderr)
    shutil.rmtree(target_dir)
    print(f"[SUCCESS] Cleaned up reference directory", file=sys.stderr)


def clone_from_json(addons_json: List[dict], target_dir: Path) -> None:
    """Clone repositories from filtered addon JSON."""
    target_dir.mkdir(parents=True, exist_ok=True)
    
    successful = 0
    failed = 0
    
    for addon in addons_json:
        github_url = addon.get("links", {}).get("github")
        if not github_url:
            name = addon.get("name", "Unknown")
            print(f"[WARNING] No GitHub URL for {name}", file=sys.stderr)
            continue
        
        if clone_repo(github_url, target_dir):
            successful += 1
        else:
            failed += 1
    
    print(f"\n[SUMMARY] {successful} successful, {failed} failed", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(
        description="Clone Meteor addon repositories for analysis"
    )
    parser.add_argument(
        "repo_url",
        nargs='?',
        help="GitHub repository URL to clone"
    )
    parser.add_argument(
        "--from-json",
        action="store_true",
        help="Read addon JSON from stdin (from filter_addons.py output)"
    )
    parser.add_argument(
        "--target",
        default=DEFAULT_TARGET,
        help=f"Target directory for cloned repos (default: {DEFAULT_TARGET})"
    )
    parser.add_argument(
        "--cleanup",
        action="store_true",
        help="Remove all cloned repositories"
    )
    
    args = parser.parse_args()
    target_dir = Path(args.target)
    
    if args.cleanup:
        cleanup_references(target_dir)
        return
    
    if args.from_json:
        try:
            addons_json = json.load(sys.stdin)
            clone_from_json(addons_json, target_dir)
        except json.JSONDecodeError as e:
            print(f"Error parsing JSON from stdin: {e}", file=sys.stderr)
            sys.exit(1)
    elif args.repo_url:
        target_dir.mkdir(parents=True, exist_ok=True)
        if clone_repo(args.repo_url, target_dir):
            print(f"\n[SUCCESS] Repository cloned to: {target_dir / args.repo_url.split('/')[-1].replace('.git', '')}")
        else:
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
