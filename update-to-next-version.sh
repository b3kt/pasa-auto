#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
POM_FILE="$PROJECT_DIR/pom.xml"

usage() {
    echo "Usage: $0 [--dry-run] [--skip-git-check] [release-version]"
    echo "  --dry-run: Show what would be done without making changes"
    echo "  --skip-git-check: Skip git status check (useful for CI environments)"
    echo "  release-version: Optional. If not provided, will extract from pom.xml"
    echo "Example: $0 1.0.0"
    echo "Example: $0 --dry-run"
    echo "Example: $0 --skip-git-check 1.0.0"
    exit 1
}

validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: Version must be in format x.y.z (e.g., 1.0.0)"
        exit 1
    fi
}

increment_version() {
    local version=$1
    local major=$(echo $version | cut -d. -f1)
    local minor=$(echo $version | cut -d. -f2)
    local patch=$(echo $version | cut -d. -f3)
    
    # Increment patch version
    patch=$((patch + 1))
    
    echo "${major}.${minor}.${patch}"
}

get_current_version() {
    if [[ ! -f "$POM_FILE" ]]; then
        echo "Error: pom.xml not found at $POM_FILE"
        exit 1
    fi
    
    grep -m1 "<version>" "$POM_FILE" | sed 's/.*<version>\(.*\)<\/version>.*/\1/'
}

is_snapshot_version() {
    local version=$1
    [[ $version == *"-SNAPSHOT" ]]
}

check_git_status() {
    local dry_run=${1:-false}
    local skip_check=${2:-false}
    
    if [[ "$skip_check" == "true" ]]; then
        echo "Skipping git status check as requested"
        return
    fi
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would check git status for uncommitted changes"
        return
    fi
    
    if [[ -n $(git status --porcelain) ]]; then
        echo "Error: Working directory is not clean. Please commit or stash changes."
        echo "Current git status:"
        git status
        echo ""
        echo "To skip this check, use: $0 --skip-git-check"
        exit 1
    fi
}

update_pom_version() {
    local new_version=$1
    local dry_run=${2:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would update pom.xml version to $new_version"
        return
    fi
    
    echo "Updating pom.xml version to $new_version..."
    
    if [[ ! -f "$POM_FILE" ]]; then
        echo "Error: pom.xml not found at $POM_FILE"
        exit 1
    fi
    
    # Update version, handle both snapshot and release versions
    if [[ $new_version == *"-SNAPSHOT" ]]; then
        sed -i "s/<version>.*<\/version>/<version>$new_version<\/version>/" "$POM_FILE"
    else
        sed -i "s/<version>.*-SNAPSHOT<\/version>/<version>$new_version<\/version>/" "$POM_FILE"
    fi
    
    if ! grep -q "<version>$new_version</version>" "$POM_FILE"; then
        echo "Error: Failed to update version in pom.xml"
        exit 1
    fi
    
    echo "Version updated successfully to $new_version"
}

create_git_tag() {
    local version=$1
    local dry_run=${2:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would create git tag v$version and commit release"
        return
    fi
    
    echo "Creating git tag v$version..."
    git add "$POM_FILE"
    git commit -m "Release version $version"
    git tag -a "v$version" -m "Release version $version"
    echo "Git tag v$version created"
}

push_to_remote() {
    local version=$1
    local dry_run=${2:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would push to remote:"
        echo "[DRY-RUN]   git push origin main"
        echo "[DRY-RUN]   git push origin v$version"
        return
    fi
    
    echo "Pushing to remote..."
    git push origin main
    git push origin "v$version"
    echo "Pushed to remote successfully"
}

generate_changelog() {
    local current_version=$1
    local previous_version=$2
    
    echo "## 🚀 Release v${current_version}"
    echo ""
    echo "### 📋 Changes"
    echo ""
    
    # Get commits between the previous tag and current HEAD
    if git rev-parse "v${previous_version}" >/dev/null 2>&1; then
        echo "### 🔄 Changes since v${previous_version}"
        echo ""
        
        # Get commit messages between tags
        local commits=$(git log --pretty=format:"- %s (%h)" "v${previous_version}..HEAD" --no-merges)
        if [[ -n "$commits" ]]; then
            echo "$commits"
        else
            echo "- No changes detected"
        fi
        echo ""
        
        # Get contributors
        local contributors=$(git log --pretty=format:"- %an" "v${previous_version}..HEAD" --no-merges | sort -u)
        if [[ -n "$contributors" ]]; then
            echo "### 👥 Contributors"
            echo ""
            echo "$contributors"
            echo ""
        fi
    else
        echo "### 🎉 Initial Release"
        echo ""
        echo "- First release of the application"
        echo ""
    fi
    
    echo "### 📦 Downloads"
    echo ""
    echo "- **Native Binary**: Quarkus native executable for Windows"
    echo "- **Electron App**: Desktop application with manager interface"
    echo ""
    echo "### 🔧 Installation"
    echo "1. Download the native binary for your platform"
    echo "2. Extract and run the executable"
    echo "3. For the desktop app, download the Electron installer"
    echo ""
    echo "---"
    echo "Built with ❤️ using Quarkus and Electron"
}

create_github_release() {
    local version=$1
    local changelog=$2
    local dry_run=${3:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would create GitHub release v$version"
        echo "[DRY-RUN] Release notes:"
        echo "$changelog" | head -10
        echo "[DRY-RUN] ... (truncated)"
        return
    fi
    
    echo "Creating GitHub release v$version..."
    
    # Create release using GitHub CLI or curl
    if command -v gh >/dev/null 2>&1; then
        echo "$changelog" | gh release create "v$version" \
            --title "Release v$version" \
            --notes-file - \
            --draft=false
        echo "GitHub release created using gh CLI"
    else
        echo "Warning: gh CLI not found. Release notes saved to changelog.md"
        echo "$changelog" > changelog.md
        echo "Please manually create the release on GitHub and copy the content from changelog.md"
    fi
}

get_previous_version() {
    local current_version=$1
    
    # Get the latest tag before current version
    local latest_tag=$(git tag --sort=-version:refname | grep -v "v${current_version}" | head -1)
    
    if [[ -n "$latest_tag" ]]; then
        # Remove 'v' prefix to get version number
        echo "${latest_tag#v}"
    else
        echo "0.0.0"  # Default for first release
    fi
}

attach_release_artifacts() {
    local version=$1
    local dry_run=${2:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would attach artifacts to GitHub release v$version"
        echo "[DRY-RUN] Artifacts to attach:"
        echo "[DRY-RUN] - artifacts/native-windows/pasa-auto-runner.exe"
        echo "[DRY-RUN] - artifacts/electron-windows/*.exe"
        return
    fi
    
    echo "Attaching artifacts to GitHub release v$version..."
    
    # Attach artifacts if they exist
    if [[ -f "artifacts/native-windows/pasa-auto-runner.exe" ]]; then
        if command -v gh >/dev/null 2>&1; then
            gh release upload "v$version" "artifacts/native-windows/pasa-auto-runner.exe"
            echo "Attached native binary"
        fi
    fi
    
    # Attach Electron artifacts
    for exe_file in artifacts/electron-windows/*.exe; do
        if [[ -f "$exe_file" ]]; then
            if command -v gh >/dev/null 2>&1; then
                gh release upload "v$version" "$exe_file"
                echo "Attached $(basename "$exe_file")"
            fi
        fi
    done
    
    # Attach nested Electron artifacts
    for exe_file in artifacts/electron-windows/**/*.exe; do
        if [[ -f "$exe_file" ]]; then
            if command -v gh >/dev/null 2>&1; then
                gh release upload "v$version" "$exe_file"
                echo "Attached $(basename "$exe_file")"
            fi
        fi
    done
    
    echo "Artifact attachment completed"
}

commit_next_version() {
    local next_version=$1
    local dry_run=${2:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would commit next development version $next_version"
        return
    fi
    
    echo "Committing next development version..."
    git add "$POM_FILE"
    git commit -m "Prepare next development version $next_version"
    echo "Next development version committed"
}

push_next_version() {
    local dry_run=${1:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would push next development version to remote"
        return
    fi
    
    echo "Pushing next development version..."
    git push origin main
    echo "Next development version pushed"
}

main() {
    local dry_run=false
    local skip_git_check=false
    local release_version=""
    local update_release_version=false
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                dry_run=true
                shift
                ;;
            --skip-git-check)
                skip_git_check=true
                shift
                ;;
            --update-release)
                update_release_version=true
                shift
                ;;
            -h|--help)
                usage
                ;;
            *)
                if [[ -z "$release_version" ]]; then
                    release_version=$1
                else
                    echo "Error: Too many arguments"
                    usage
                fi
                shift
                ;;
        esac
    done
    
    # Get current version from pom.xml if not provided
    if [[ -z "$release_version" ]]; then
        local current_version=$(get_current_version)
        if [[ -z "$current_version" ]]; then
            echo "Error: Could not extract current version from pom.xml"
            exit 1
        fi
        
        if is_snapshot_version "$current_version"; then
            # Remove -SNAPSHOT suffix to get release version
            release_version=${current_version%-SNAPSHOT}
        else
            echo "Error: Current version ($current_version) is not a SNAPSHOT version"
            echo "Please provide the release version explicitly"
            exit 1
        fi
    fi
    
    validate_version "$release_version"
    check_git_status "$dry_run" "$skip_git_check"
    
    # Calculate next development version
    local next_version=$(increment_version "$release_version")
    local next_snapshot_version="${next_version}-SNAPSHOT"
    
    echo ""
    echo "=== Version Update Process Summary ==="
    echo "Current version: $(get_current_version)"
    echo "Release version: $release_version"
    echo "Next development version: $next_snapshot_version"
    echo "Update release version: $update_release_version"
    echo "Dry run: $dry_run"
    echo ""
    
    if [[ "$dry_run" == "true" ]]; then
        echo "=== DRY RUN MODE - No changes will be made ==="
    fi
    
    if [[ "$update_release_version" == "true" ]]; then
        echo "Starting version update process for release $release_version"
        
        # 1. Update to release version
        update_pom_version "$release_version" "$dry_run"
        
        # 2. Create release commit and tag
        create_git_tag "$release_version" "$dry_run"
        
        # 3. Push release to remote
        push_to_remote "$release_version" "$dry_run"
        
        # 4. Generate changelog
        local previous_version=$(get_previous_version "$release_version")
        local changelog=$(generate_changelog "$release_version" "$previous_version")
        
        # 5. Create GitHub release with changelog
        create_github_release "$release_version" "$changelog" "$dry_run"
        
        # 6. Attach artifacts to GitHub release
        attach_release_artifacts "$release_version" "$dry_run"
        
        # 7. Update to next development version
        update_pom_version "$next_snapshot_version" "$dry_run"
        
        # 8. Commit next development version
        commit_next_version "$next_snapshot_version" "$dry_run"
        
        # 9. Push next development version
        push_next_version "$dry_run"
        
        echo ""
        echo "=== Version update completed successfully! ==="
        echo "Release version: $release_version"
        echo "Next development version: $next_snapshot_version"
        echo "Tag: v$release_version"
        echo "GitHub release: Created with changelog and artifacts"
        
    else
        echo "Starting next development version update process"
        
        # 1. Update to next development version only
        update_pom_version "$next_snapshot_version" "$dry_run"
        
        # 2. Commit next development version
        commit_next_version "$next_snapshot_version" "$dry_run"
        
        # 3. Push next development version
        push_next_version "$dry_run"
        
        echo ""
        echo "=== Next development version update completed! ==="
        echo "Next development version: $next_snapshot_version"
    fi
    
    echo ""
    
    if [[ "$dry_run" == "true" ]]; then
        echo "This was a dry run. No actual changes were made."
        echo "Run without --dry-run to perform the actual version update."
    else
        echo "Version update has been pushed to remote repository."
    fi
}

main "$@"
