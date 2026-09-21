import importlib.util
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location("verify_web_apk", Path(__file__).resolve().parents[1] / "verify_web_apk.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class VerifyWebApkTest(unittest.TestCase):
    def test_packaged_resource_contract(self):
        for scenario in ("matching", "missing", "changed", "no_index"):
            with self.subTest(scenario=scenario), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                resources = root / "resources"
                resources.mkdir()
                if scenario != "no_index":
                    (resources / "index.html").write_text("index")
                (resources / "_helper.js").write_text("helper")
                apk = root / "test.apk"
                with ZipFile(apk, "w") as archive:
                    archive.writestr("web/index.html", "index")
                    if scenario != "missing":
                        archive.writestr("web/_helper.js", "changed" if scenario == "changed" else "helper")
                if scenario == "matching":
                    self.assertEqual(2, module.verify(apk, resources))
                else:
                    with self.assertRaises(ValueError):
                        module.verify(apk, resources)
