#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
POM_FILE="$PROJECT_DIR/pom.xml"

usage() {
    echo "Usage: $0 <release-version>"
    echo "Example: $0 1.0.0"
    exit 1
}

validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: Version must be in format x.y.z (e.g., 1.0.0)"
        exit 1
    fi
}

check_git_status() {
    if [[ -n $(git status --porcelain) ]]; then
        echo "Error: Working directory is not clean. Please commit or stash changes."
        exit 1
    fi
}

update_pom_version() {
    local new_version=$1
    echo "Updating pom.xml version to $new_version..."
    
    if [[ ! -f "$POM_FILE" ]]; then
        echo "Error: pom.xml not found at $POM_FILE"
        exit 1
    fi
    
    sed -i "s/<version>.*-SNAPSHOT<\/version>/<version>$new_version<\/version>/" "$POM_FILE"
    
    if ! grep -q "<version>$new_version</version>" "$POM_FILE"; then
        echo "Error: Failed to update version in pom.xml"
        exit 1
    fi
    
    echo "Version updated successfully to $new_version"
}

run_tests() {
    echo "Running tests..."
    mvn clean test
    echo "Tests completed successfully"
}

build_native_image() {
    echo "Building native image..."
    mvn clean package -Pnative
    echo "Native image built successfully"
}

create_git_tag() {
    local version=$1
    echo "Creating git tag v$version..."
    git add "$POM_FILE"
    git commit -m "Release version $version"
    git tag -a "v$version" -m "Release version $version"
    echo "Git tag v$version created"
}

main() {
    if [[ $# -ne 1 ]]; then
        usage
    fi
    
    local release_version=$1
    local next_snapshot_version=$2
    
    validate_version "$release_version"
    check_git_status
    
    echo "Starting release process for version $release_version"
    
    update_pom_version "$release_version"
    run_tests
    build_native_image
    create_git_tag "$release_version"

    git push origin "main"
    git push origin "v$release_version"

    update_pom_version "$next_snapshot_version"

    echo ""
    echo "Release completed successfully!"
    echo "Version: $release_version"
    echo "Native binary: target/quarkus-quasar-$release_version-runner"
    echo ""
    echo "To push the release:"
    echo "  git push origin main"
    echo "  git push origin v$release_version"


}

main "$@"
