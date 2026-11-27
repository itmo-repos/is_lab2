#!/usr/bin/env bash
set -e

project="$(dirname "$0")/.."
source="$project/src/main/java/ru/itmo/is"
classes="$project/target/import-generator"

javac -encoding UTF-8 -d "$classes" \
    "$source"/model/{Color,Country,Difficulty}.java \
    "$source/importfile/LabWorkRecord.java" \
    "$project/scripts/GenerateImportSamples.java"
java -cp "$classes" GenerateImportSamples "${1:-$project/target/import-samples}"
