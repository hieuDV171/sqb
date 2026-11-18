"""File-preservation/rollback tests using a real Caddy adapter.

Run: python test_ensure_caddy.py --caddy /path/to/caddy
Only service reload and module provisioning are stubbed; all parsing is real.
"""

import argparse
import contextlib
import importlib.util
import io
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


HERE = Path(__file__).parent
SPEC = importlib.util.spec_from_file_location("ensure_caddy", HERE / "ensure-caddy.py")
installer = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(installer)
CADDY = "caddy"


class CaddyInstallTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.config = self.directory / "Caddyfile"
        # Unknown project content, comments, a brace in a string and a relative
        # import must survive byte-for-byte. Do not parse/rewrite Caddyfile text.
        self.foreign = ('# managed by another team\n'
                        'import other-project.caddy\n'
                        'other.example.org {\n'
                        '  respond "foreign {uri}"\n}\n')
        (self.directory / "other-project.caddy").write_text(
            'second.example.org {\n reverse_proxy localhost:3010\n}\n', encoding="utf-8")
        self.config.write_bytes(self.foreign.encode())
        self.reloads = []
        self.real_run = installer.run
        self.validations = []

        def run(command):
            if "validate" in command:
                self.validations.append(command)
                return ""  # no host module provisioning/network in file tests
            return self.real_run(command)

        self.mock_run = patch.object(installer, "run", side_effect=run)
        self.mock_run.start()
        self.addCleanup(self.mock_run.stop)
        self.output = contextlib.redirect_stdout(io.StringIO())
        self.output.__enter__()
        self.addCleanup(self.output.__exit__, None, None, None)

    def install(self, site="public", apply=True, reload_service=None):
        source, name, hosts = installer.SITES[site]
        return installer.ensure_site(
            self.config, HERE / source, self.directory / name, hosts, CADDY, apply,
            reload_service or (lambda: self.reloads.append(True)))

    def test_missing_appends_once_and_preserves_other_teams(self):
        before = self.config.read_bytes()
        foreign_hosts = {"other.example.org", "second.example.org"}
        foreign_before = installer.project(installer.adapt(CADDY, self.config), foreign_hosts)
        self.assertTrue(self.install())
        after = self.config.read_bytes()
        self.assertTrue(after.startswith(before))
        self.assertEqual(after.count(b"import " +
                         (self.directory / "sqb-public.caddy").as_posix().encode()), 1)
        self.assertEqual(self.reloads, [True])
        self.assertEqual(installer.project(installer.adapt(CADDY, self.config), foreign_hosts),
                         foreign_before)
        self.assertFalse(self.install())
        self.assertEqual(self.config.read_bytes(), after)
        self.assertEqual(self.reloads, [True])

    def test_check_does_not_install_or_reload(self):
        before = self.config.read_bytes()
        self.assertFalse(self.install(apply=False))
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse((self.directory / "sqb-public.caddy").exists())
        self.assertFalse(self.reloads)

    def test_admin_install_keeps_listener_on_loopback(self):
        self.assertTrue(self.install("admin"))
        native = installer.adapt(CADDY, self.config)
        admin = installer.project(native, {"admin.hieudv.id.vn"})
        self.assertEqual(admin[0]["listen"], ["127.0.0.1:9080"])
        self.assertFalse(self.install("admin"))
        self.assertEqual(self.reloads, [True])

    def test_publicly_bound_admin_listener_is_a_conflict(self):
        listener = (HERE / "admin-listener.caddy").read_text(encoding="utf-8")
        self.config.write_text(self.foreign + listener.replace("bind 127.0.0.1",
                                                               "bind 0.0.0.0"),
                               encoding="utf-8")
        before = self.config.read_bytes()
        with self.assertRaisesRegex(installer.ConfigError, "Existing SQB host"):
            self.install("admin")
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse(self.reloads)

    def test_existing_inline_cutover_needs_no_migration_or_reload(self):
        # An unrelated handle changes generated group IDs in the Caddy adapter.
        self.config.write_text(self.foreign + 'aaa.example.org {\n'
            ' handle /api/* {\n respond "api"\n }\n handle {\n respond "site"\n }\n}\n' +
            (HERE / "public-site.caddy").read_text(encoding="utf-8") +
            (HERE / "admin-listener.caddy").read_text(encoding="utf-8"), encoding="utf-8")
        before = self.config.read_bytes()
        self.assertFalse(self.install())
        self.assertFalse(self.install("admin"))
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse(self.reloads)

    def test_existing_wildcard_import_is_not_duplicated(self):
        self.config.write_text(self.foreign + 'import *.caddy\n', encoding="utf-8")
        # Remove the explicitly imported other project from the glob by moving it
        # to a non-caddy suffix, so only the new SQB file gets picked up twice if buggy.
        other = self.directory / "other-project.caddy"
        other.rename(self.directory / "other-project.conf")
        self.config.write_text(self.config.read_text().replace("other-project.caddy",
                                                              "other-project.conf"))
        before = self.config.read_bytes()
        self.assertTrue(self.install())
        self.assertEqual(self.config.read_bytes(), before)
        self.assertEqual(self.reloads, [True])

    def test_conflicting_inline_site_is_not_appended_or_overwritten(self):
        self.config.write_text(self.foreign +
            'hieudv.id.vn {\n reverse_proxy localhost:9999\n}\n', encoding="utf-8")
        before = self.config.read_bytes()
        with self.assertRaisesRegex(installer.ConfigError, "Existing SQB host"):
            self.install()
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse((self.directory / "sqb-public.caddy").exists())
        self.assertFalse(self.reloads)

    def test_occupied_destination_is_not_overwritten(self):
        target = self.directory / "sqb-public.caddy"
        target.write_text("# owned by someone else\n", encoding="utf-8")
        with self.assertRaisesRegex(installer.ConfigError, "will not be overwritten"):
            self.install()
        self.assertEqual(target.read_text(), "# owned by someone else\n")
        self.assertFalse(self.reloads)

    def test_validation_failure_undoes_only_our_append(self):
        before = self.config.read_bytes()
        with patch.object(installer, "run", side_effect=lambda command:
                          (_ for _ in ()).throw(installer.ConfigError("bad validation"))
                          if "validate" in command else self.real_run(command)):
            with self.assertRaisesRegex(installer.ConfigError, "bad validation"):
                self.install()
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse((self.directory / "sqb-public.caddy").exists())
        self.assertFalse(self.reloads)

    def test_reload_failure_rolls_back_files(self):
        before = self.config.read_bytes()
        with self.assertRaisesRegex(installer.ConfigError, "reload failed"):
            self.install(reload_service=lambda:
                         (_ for _ in ()).throw(installer.ConfigError("reload failed")))
        self.assertEqual(self.config.read_bytes(), before)
        self.assertFalse((self.directory / "sqb-public.caddy").exists())

    def test_concurrent_edit_is_never_replaced_by_old_backup(self):
        def concurrent_reload_failure():
            with self.config.open("ab") as output:
                output.write(b'\nnew-team.example.org {\n respond "new"\n}\n')
            raise installer.ConfigError("reload failed")

        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(installer.ConfigError):
                self.install(reload_service=concurrent_reload_failure)
        self.assertIn(b"new-team.example.org", self.config.read_bytes())
        self.assertTrue((self.directory / "sqb-public.caddy").exists())


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--caddy", default=os.environ.get("CADDY_BIN", "caddy"))
    args, remaining = parser.parse_known_args()
    CADDY = args.caddy
    unittest.main(argv=[__file__, *remaining])
