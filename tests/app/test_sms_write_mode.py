import os
import subprocess
import unittest
from pathlib import Path


DELETE = Path(__file__).resolve().parents[2] / 'shared/src/androidMain/res/raw/delete_sms_row.sh'
MOCKS = """
id() { echo "$TEST_UID"; }
cmd() {
    case "$2" in
        get) printf '%s\\n' "$TEST_STATE" ;;
        set) echo "$5 $6 $7" ;;
        *) return 1 ;;
    esac
}
content() { echo "$*"; return "$TEST_RESULT"; }
. "$TEST_SCRIPT"
"""


class SmsWriteModeTest(unittest.TestCase):
    delete = 'delete --uri content://sms/123 --user 10'

    def execute(self, state, expected, *, uid='2000', status=0, content_result=0):
        result = subprocess.run(
            ['sh', '-c', MOCKS, 'delete-sms', 'content://sms/123', '10'],
            env={**os.environ, 'TEST_UID': uid, 'TEST_STATE': state,
                 'TEST_RESULT': str(content_result), 'TEST_SCRIPT': str(DELETE)},
            capture_output=True, text=True,
        )
        self.assertEqual(status, result.returncode, result.stderr)
        self.assertEqual(expected, result.stdout.splitlines())

    def test_ignored_package_is_restored_after_deletion(self):
        self.execute('WRITE_SMS: ignore; time=+1m ago', [
            'com.android.shell WRITE_SMS allow', self.delete, 'com.android.shell WRITE_SMS ignore'])

    def test_default_is_restored_on_provider_failure(self):
        self.execute('No operations.\nDefault mode: ignore', [
            'com.android.shell WRITE_SMS allow', self.delete, 'com.android.shell WRITE_SMS ignore'],
            status=7, content_result=7)

    def test_explicit_uid_override_is_restored_at_uid_scope(self):
        self.execute('Uid mode: WRITE_SMS: deny\nWRITE_SMS: allow', [
            '2000 WRITE_SMS allow', self.delete, '2000 WRITE_SMS deny'])

    def test_existing_root_grant_needs_no_change(self):
        self.execute('Uid mode: WRITE_SMS: allow', [self.delete], uid='0')

    def test_unknown_mode_stops_before_changing_anything(self):
        self.execute('Unexpected response', [], status=1)


if __name__ == '__main__':
    unittest.main()
