import importlib.util
import io
from contextlib import redirect_stdout
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch


path = Path(__file__).resolve().parents[2] / "skills/seeclap-tv/scripts/cache.py"
spec = importlib.util.spec_from_file_location("tv_cache", path)
cache = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cache)


def result(stdout="", stderr="", code=0):
    return subprocess.CompletedProcess([], code, stdout, stderr)


def storage(available):
    return result(f"Filesystem 1K-blocks Used Available Use% Mounted on\n"
                  f"/dev/block/dm-4\n 2000 1000 {available} 50% /data")


class CacheTest(unittest.TestCase):
    def run_helper(self, responses, clean=True):
        args = ["cache.py", "--serial", "chosen-tv"]
        if clean:
            args.append("--clean")
        output = io.StringIO()
        with patch.object(sys, "argv", args), redirect_stdout(output):
            with patch.object(subprocess, "run", side_effect=responses) as run:
                cache.main()
        commands = [call.args[0] for call in run.call_args_list]
        for command in commands:
            self.assertEqual(command[:4], ["adb", "-s", "chosen-tv", "shell"])
        return output.getvalue(), [command[4:] for command in commands]

    def setUp(self):
        self.tv = [result("feature:android.software.leanback"), result("Test TV"), storage(1250)]

    def test_measure_only_keeps_tv_unchanged(self):
        output, commands = self.run_helper(self.tv, clean=False)
        self.assertEqual(commands, [["pm", "list", "features"], ["getprop", "ro.product.model"],
                                    ["df", "-k", "/data"]])
        self.assertIn("Free before: 1250 KiB", output)

    def test_clean_measures_change_even_when_space_decreases(self):
        output, commands = self.run_helper([*self.tv, result(), storage(1200)])
        self.assertEqual(commands[-2:], [["pm", "trim-caches", "999999999999"], ["df", "-k", "/data"]])
        self.assertIn("Change: -50 KiB", output)

    def test_refusal_on_stderr_with_zero_exit(self):
        with self.assertRaisesRegex(ValueError, "permission denied"):
            self.run_helper([*self.tv, result(stderr="Error: permission denied")])

    def test_non_tv_stops_before_clean(self):
        with self.assertRaisesRegex(ValueError, "Android TV features"):
            self.run_helper([result("feature:android.hardware.touchscreen")])

    def test_failed_final_measurement_is_reported(self):
        with self.assertRaisesRegex(ValueError, "offline"):
            self.run_helper([*self.tv, result(), result(stderr="device offline", code=1)])


if __name__ == "__main__":
    unittest.main()
