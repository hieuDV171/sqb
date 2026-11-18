#!/usr/bin/env python3
"""Install a missing SQB site without replacing a shared Caddyfile or existing sites.

Default: read-only check. --apply creates an absent SQB file and, if necessary,
appends one import. Conflicting existing SQB configurations require manual review.
Only the Linux/VPS entry point takes an advisory lock; helpers are portable for tests.
"""

import argparse
import copy
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


SITES = {
    "admin": ("admin-listener.caddy", "sqb-admin.caddy", {"admin.hieudv.id.vn"}),
    "public": ("public-site.caddy", "sqb-public.caddy",
               {"hieudv.id.vn", "www.hieudv.id.vn", "snsqb.duckdns.org"}),
}


class ConfigError(RuntimeError):
    pass


def run(command):
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise ConfigError(f"Command failed: {' '.join(command)}\n{result.stderr.strip()}")
    return result.stdout


def adapt(caddy, path, imported_sources=()):
    config = json.loads(run([caddy, "adapt", "--adapter", "caddyfile", "--config", str(path)]))
    # file_server automatically hides the input Caddyfile's absolute path. It
    # changes when the same site is inline vs imported; keep all other hide rules.
    input_paths = {os.path.normcase(os.path.abspath(item))
                   for item in (path, *imported_sources)}

    def strip_generated_hide(value):
        if isinstance(value, dict):
            if value.get("handler") == "file_server" and "hide" in value:
                value["hide"] = [item for item in value["hide"]
                                 if os.path.normcase(os.path.abspath(item)) not in input_paths]
                if not value["hide"]:
                    del value["hide"]
            for child in value.values():
                strip_generated_hide(child)
        elif isinstance(value, list):
            for child in value:
                strip_generated_hide(child)

    strip_generated_hide(config)
    return config


def host_match(value, hosts):
    """Conservatively flag overlapping/wildcard host matchers, even nested ones."""
    if isinstance(value, dict):
        for host in value.get("host", []):
            for owned in hosts:
                if host == owned or (host.startswith("*.") and
                                     owned.endswith(host[1:])):
                    return True
        return any(host_match(item, hosts) for item in value.values())
    if isinstance(value, list):
        return any(host_match(item, hosts) for item in value)
    return False


def project(config, hosts):
    """Keep complete host routes and their actual listen addresses, not server IDs."""
    sites = []
    servers = config.get("apps", {}).get("http", {}).get("servers", {})
    for server in servers.values():
        for route in server.get("routes", []):
            if host_match(route, hosts):
                sites.append({"listen": sorted(server.get("listen", [])),
                              "route": copy.deepcopy(route)})
    # Caddy's handle group numbers vary when unrelated sites are added.
    groups = {}

    def normalize(value):
        if isinstance(value, dict):
            return {key: (groups.setdefault(item, f"group{len(groups)}")
                          if key == "group" else
                          sorted(item) if key == "host" else normalize(item))
                    for key, item in sorted(value.items())}
        if isinstance(value, list):
            return [normalize(item) for item in value]
        return value

    return normalize(sites)


def snapshot(path):
    if path.is_symlink():
        raise ConfigError(f"Refusing symlink: {path}")
    return path.read_bytes() if path.exists() else None


def unchanged(path, expected):
    if snapshot(path) != expected:
        raise ConfigError(f"File changed by another administrator; retry after review: {path}")


def ensure_site(config, source, target, hosts, caddy="caddy", apply=False,
                reload_service=None):
    original = snapshot(config)
    if original is None:
        raise ConfigError(f"Shared Caddyfile must already exist: {config}")
    target_before = snapshot(target)
    desired = source.read_bytes()
    current_json = adapt(caddy, config, (target,))
    expected = project(adapt(caddy, source), hosts)
    if not expected:
        raise ConfigError("The SQB source contains no expected site routes.")
    existing = project(current_json, hosts)
    if existing == expected:
        run([caddy, "validate", "--adapter", "caddyfile", "--config", str(config)])
        print("SQB configuration is already present. No files changed; no reload.")
        return False
    if existing:
        raise ConfigError("Existing SQB host configuration differs from the supplied site. "
                          "No append/overwrite performed. Review only the SQB block/import.")
    if target_before is not None and target_before != desired:
        raise ConfigError(f"Existing file differs; it will not be overwritten: {target}")
    if not apply:
        print(f"SQB configuration is missing. --apply will install {target.name} "
              "and append an import only if the existing imports do not load it.")
        return False

    unchanged(config, original)
    unchanged(target, target_before)
    created = False
    appended = None
    backup = None
    try:
        if target_before is None:
            # Publish a complete file with an exclusive hard link. A failed write
            # cannot leave a partial file picked up by someone else's glob import.
            with tempfile.NamedTemporaryFile(prefix=".sqb-site-", dir=target.parent,
                                             delete=False) as output:
                staged = Path(output.name)
                output.write(desired)
                output.flush()
                os.fsync(output.fileno())
            try:
                staged.chmod(0o644)
                os.link(staged, target)
                created = True
            finally:
                staged.unlink()
        unchanged(config, original)
        # A pre-existing wildcard import may already include the new file.
        after_install = project(adapt(caddy, config, (target,)), hosts)
        if not after_install:
            suffix = (b"" if original.endswith(b"\n") else b"\n")
            suffix += f"\nimport {target.as_posix()}\n".encode()
            with tempfile.NamedTemporaryFile(prefix=".Caddyfile.before-sqb-",
                                             dir=config.parent, delete=False) as output:
                backup = Path(output.name)
            shutil.copy2(config, backup)
            with config.open("r+b") as output:
                if output.read() != original:
                    raise ConfigError("Caddyfile changed before append; no append performed.")
                appended = original + suffix
                output.write(suffix)
                output.flush()
                os.fsync(output.fileno())
        elif after_install != expected:
            raise ConfigError("An existing import produced conflicting SQB routes.")

        final_json = adapt(caddy, config, (target,))
        if project(final_json, hosts) != expected:
            raise ConfigError("Installed SQB routes do not match the required configuration.")
        run([caddy, "validate", "--adapter", "caddyfile", "--config", str(config)])
        unchanged(config, appended if appended is not None else original)
        unchanged(target, desired)
        if reload_service:
            reload_service()
    except BaseException:
        # Remove only our append, and only if no one has edited the file since.
        # Never restore a whole old snapshot over another administrator's changes.
        root_restored = appended is None
        if appended is not None:
            try:
                with config.open("r+b") as output:
                    if output.read() == appended:
                        output.truncate(len(original))
                        output.flush()
                        os.fsync(output.fileno())
                        root_restored = True
            except OSError:
                pass
        if created and root_restored and snapshot(target) == desired:
            target.unlink()
        if not root_restored:
            print("Concurrent edit detected: keeping current files for manual review; "
                  "no old Caddyfile snapshot was restored.", file=sys.stderr)
        raise
    print(f"Installed {target}. Shared Caddyfile was "
          f"{'only appended to' if appended is not None else 'not modified'}.")
    if backup:
        print(f"Reference backup (do not blindly restore on a shared VPS): {backup}")
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--site", choices=SITES, required=True)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--config", type=Path, default=Path("/etc/caddy/Caddyfile"))
    args = parser.parse_args()
    if os.name != "posix" or os.geteuid() != 0:
        parser.error("Run this installer with sudo on the Linux VPS.")
    import fcntl
    config = args.config.absolute()
    source_name, target_name, hosts = SITES[args.site]
    # All SQB invocations share this lock. Other administrators should coordinate
    # using the same lock; independent manual writes cannot be made transactional.
    with (config.parent / ".sqb-caddy.lock").open("a") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        ensure_site(config, Path(__file__).parent / source_name,
                    config.parent / target_name, hosts, apply=args.apply,
                    reload_service=lambda: run(["systemctl", "reload", "caddy"]))


if __name__ == "__main__":
    try:
        main()
    except (ConfigError, OSError, ValueError) as error:
        print(error, file=sys.stderr)
        sys.exit(1)
