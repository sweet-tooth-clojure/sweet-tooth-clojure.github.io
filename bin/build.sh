#!/usr/bin/env bash
set -euo pipefail

tmp_dir=$(mktemp -d -t st-docs)
git co dev
antora --fetch --to-dir=$tmp_dir antora-playbook.yml

git co master
cp -r $tmp_dir/* ./
touch .nojekyll

git add .
