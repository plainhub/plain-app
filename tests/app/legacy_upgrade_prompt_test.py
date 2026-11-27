import os
import re
import subprocess
import time
import unittest
from xml.etree import ElementTree


PACKAGE = "com.ismartcoding.plain.debug"


def adb(*arguments):
    return subprocess.check_output(["adb", "-s", os.environ["ANDROID_SERIAL"], *arguments], text=True)


def wait_for_text(text):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        screen = adb("exec-out", "uiautomator", "dump", "/dev/tty")
        if "<?xml" in screen and "</hierarchy>" in screen:
            xml = screen[screen.index("<?xml"):screen.index("</hierarchy>") + len("</hierarchy>")]
            matches = [node for node in ElementTree.fromstring(xml).iter("node") if text in node.get("text", "")]
            if matches:
                return matches
    raise AssertionError(f"Missing text on the upgrade screen: {text}")


class LegacyUpgradePromptTest(unittest.TestCase):
    def test_cold_start_offers_import_with_only_a_settings_backup(self):
        self.assertEqual(adb("shell", "getprop", "ro.kernel.qemu").strip(), "1")
        adb("shell", "am", "force-stop", PACKAGE)
        backup = "files/datastore/settings.preferences_pb.bak"
        # A failed assertion leaves the empty fixture available for the next build.
        adb("shell", "run-as", PACKAGE, "sh", "-c",
            f"'if test -f {backup}; then test ! -s {backup}; else "
            f"test ! -e files/datastore && test ! -e files/plain.db && "
            f"mkdir -p files/datastore && touch {backup}; fi'")
        adb("shell", "am", "start", "-W", "-n", PACKAGE + "/com.ismartcoding.plain.MainActivity")
        wait_for_text("Import your old app data.")
        adb("shell", "run-as", PACKAGE, "sh", "-c",
            "'mkdir -p databases && test ! -s databases/plain.db && touch databases/plain.db'")
        button = wait_for_text("Data upgrade")[-1]
        left, top, right, bottom = map(int, re.findall(r"\d+", button.get("bounds")))
        adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
        wait_for_text("Legacy database schema 0 cannot be upgraded")


if __name__ == "__main__":
    unittest.main()
