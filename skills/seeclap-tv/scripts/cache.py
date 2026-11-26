"""Measure available storage or ask Android to trim caches on one authorized TV."""

import argparse
import re
import subprocess
import sys


def adb_shell(adb, serial, *args, timeout=30):
    result = subprocess.run(
        [adb, "-s", serial, "shell", *args],
        capture_output=True, text=True, timeout=timeout, check=False,
    )
    output = result.stdout.strip()
    diagnostic = "\n".join(part for part in (output, result.stderr.strip()) if part)
    error_line = re.search(r"^\s*(?:Error|Failure|Exception|(?:java\.lang\.)?SecurityException)\b",
                           diagnostic, re.M | re.I)
    if result.returncode or error_line:
        raise ValueError(diagnostic or f"ADB command failed ({result.returncode})")
    return output


def available_kib(adb, serial):
    output = adb_shell(adb, serial, "df", "-k", "/data")
    # Android's df identifies the available column by name, even if the device name wraps.
    lines = output.splitlines()
    for index, line in enumerate(lines):
        header = re.sub(r"mounted\s+on", "mountpoint", line.lower()).split()
        for name in ("available", "avail"):
            if name in header:
                # Count from the right so wrapped device names do not shift the column.
                position = header.index(name) - len(header)
                for row in reversed(lines[index + 1:]):
                    fields = row.split()
                    if len(fields) >= -position and fields[position].isdigit():
                        return int(fields[position])
    raise ValueError("Could not read available KiB from df -k /data")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Exact TV identifier from adb devices -l")
    parser.add_argument("--adb", default="adb", help="Path to Android SDK adb")
    parser.add_argument("--clean", action="store_true", help="Also request Android cache trimming")
    args = parser.parse_args()

    features = adb_shell(args.adb, args.serial, "pm", "list", "features").splitlines()
    if not {"feature:android.software.leanback", "feature:android.hardware.type.television"}.intersection(features):
        raise ValueError("Selected device does not report Android TV features")
    model = adb_shell(args.adb, args.serial, "getprop", "ro.product.model")
    before = available_kib(args.adb, args.serial)
    print(f"TV: {model}\nFree before: {before} KiB", flush=True)
    if args.clean:
        adb_shell(args.adb, args.serial, "pm", "trim-caches", "999999999999", timeout=180)
        after = available_kib(args.adb, args.serial)
        print(f"Free after: {after} KiB\nChange: {after - before:+d} KiB")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, subprocess.TimeoutExpired) as error:
        sys.exit(str(error))
