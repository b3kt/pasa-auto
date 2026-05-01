#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
POM_FILE="$PROJECT_DIR/pom.xml"

usage() {
    echo "Usage: $0 [--dry-run] [release-version]"
    echo "  --dry-run: Show what would be done without making changes"
    echo "  release-version: Optional. If not provided, will extract from pom.xml"
    echo "Example: $0 1.0.0"
    echo "Example: $0 --dry-run"
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
    if [[ -n $(git status --porcelain) ]]; then
        echo "Error: Working directory is not clean. Please commit or stash changes."
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

run_tests() {
    local dry_run=${1:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would run tests: mvn clean test"
        return
    fi
    
    echo "Running tests..."
    mvn clean test
    echo "Tests completed successfully"
}

build_native_image() {
    local dry_run=${1:-false}
    
    if [[ "$dry_run" == "true" ]]; then
        echo "[DRY-RUN] Would build native image: mvn clean package -Pnative"
        return
    fi
    
    echo "Building native image..."
    mvn clean package -Pnative
    echo "Native image built successfully"
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

main() {
    local dry_run=false
    local release_version=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                dry_run=true
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
    
    if [[ "$dry_run" != "true" ]]; then
        check_git_status
    fi
    
    # Calculate next development version
    local next_version=$(increment_version "$release_version")
    local next_snapshot_version="${next_version}-SNAPSHOT"
    
    echo ""
    echo "=== Release Process Summary ==="
    echo "Current version: $(get_current_version)"
    echo "Release version: $release_version"
    echo "Next development version: $next_snapshot_version"
    echo "Dry run: $dry_run"
    echo ""
    
    if [[ "$dry_run" == "true" ]]; then
        echo "=== DRY RUN MODE - No changes will be made ==="
    fi
    
    echo "Starting release process for version $release_version"
    
    # 1. Update to release version
    update_pom_version "$release_version" "$dry_run"
    
    # 2. Run tests
    run_tests "$dry_run"
    
    # 3. Build native image
    build_native_image "$dry_run"
    
    # 4. Create release commit and tag
    create_git_tag "$release_version" "$dry_run"
    
    # 5. Push release to remote
    push_to_remote "$release_version" "$dry_run"
    
    # 6. Update to next development version
    update_pom_version "$next_snapshot_version" "$dry_run"
    
    # 7. Commit next development version
    commit_next_version "$next_snapshot_version" "$dry_run"
    
    # 8. Push next development version
    if [[ "$dry_run" != "true" ]]; then
        echo "Pushing next development version..."
        git push origin main
        echo "Next development version pushed"
    else
        echo "[DRY-RUN] Would push next development version to remote"
    fi

    echo ""
    echo "=== Release completed successfully! ==="
    echo "Release version: $release_version"
    echo "Next development version: $next_snapshot_version"
    echo "Native binary: target/quarkus-quasar-$release_version-runner"
    echo "Tag: v$release_version"
    echo ""
    
    if [[ "$dry_run" == "true" ]]; then
        echo "This was a dry run. No actual changes were made."
        echo "Run without --dry-run to perform the actual release."
    else
        echo "Release has been pushed to remote repository."
        echo "You can now download the native binary from the releases section."
    fi
}

main "$@"
