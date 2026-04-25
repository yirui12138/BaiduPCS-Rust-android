import json
import os
import shutil
import subprocess
import sys
import tempfile
import time

if os.name == "nt":
    import winreg


HOST_LINKER = (
    "C:/Users/zmf20/.rustup/toolchains/stable-x86_64-pc-windows-msvc/"
    "lib/rustlib/x86_64-pc-windows-msvc/bin/rust-lld.exe"
)

HOST_NATIVE_FLAGS = [
    "-Lnative=D:/瀹為獙/tools/xwin-cache/unpack/"
    "Microsoft.VC.14.44.17.14.CRT.x64.Desktop.base.vsix/lib/x64",
    "-Lnative=D:/瀹為獙/tools/xwin-cache/unpack/"
    "Microsoft.VC.14.44.17.14.CRT.x64.Store.base.vsix/lib/x64",
    "-Lnative=D:/瀹為獙/tools/xwin-cache/unpack/ucrt.msi/lib/ucrt/x64",
    "-Lnative=D:/瀹為獙/tools/xwin-store-lib-lowercase",
    "-Lnative=D:/瀹為獙/tools/xwin-sdk-lib-lowercase",
]

PROBE_CACHE_PATH = os.path.join(
    tempfile.gettempdir(),
    "baidupcs-rust-smart-app-control-probe.json",
)
TRACE_LOG_PATH = os.path.join(
    tempfile.gettempdir(),
    "baidupcs-rust-smart-app-control-trace.log",
)
PROBE_CACHE_TTL_SECONDS = 15 * 60
PROBE_SCHEMA_VERSION = 1


def has_target(args: list[str]) -> bool:
    return "--target" in args


def is_compile_invocation(args: list[str]) -> bool:
    return "--crate-name" in args


def has_linker_override(args: list[str]) -> bool:
    for index, arg in enumerate(args):
        if arg.startswith("-Clinker="):
            return True
        if arg == "-C" and index + 1 < len(args) and args[index + 1].startswith("linker="):
            return True
    return False


def trace(message: str) -> None:
    if os.environ.get("BAIDUPCS_TRACE_WRAPPER") != "1":
        return

    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    try:
        with open(TRACE_LOG_PATH, "a", encoding="utf-8") as handle:
            handle.write(f"[{timestamp}] {message}\n")
    except OSError:
        pass


def read_smart_app_control_state() -> int | None:
    if os.name != "nt":
        return None

    try:
        with winreg.OpenKey(
            winreg.HKEY_LOCAL_MACHINE,
            r"SYSTEM\CurrentControlSet\Control\CI\Policy",
        ) as key:
            state, _ = winreg.QueryValueEx(key, "VerifiedAndReputablePolicyState")
    except OSError:
        return None

    return state


def smart_app_control_enforced() -> bool:
    return read_smart_app_control_state() == 1


def load_probe_cache() -> tuple[bool, bool, str]:
    if os.environ.get("BAIDUPCS_FORCE_SAC_PROBE") == "1":
        return False, False, ""

    try:
        with open(PROBE_CACHE_PATH, "r", encoding="utf-8") as handle:
            payload = json.load(handle)
    except (OSError, ValueError):
        return False, False, ""

    if payload.get("schema") != PROBE_SCHEMA_VERSION:
        return False, False, ""

    timestamp = payload.get("timestamp")
    if not isinstance(timestamp, (int, float)):
        return False, False, ""

    if (time.time() - timestamp) > PROBE_CACHE_TTL_SECONDS:
        return False, False, ""

    blocked = payload.get("blocked")
    details = payload.get("details")
    if not isinstance(blocked, bool) or not isinstance(details, str):
        return False, False, ""

    return True, blocked, details


def store_probe_cache(blocked: bool, details: str) -> None:
    payload = {
        "schema": PROBE_SCHEMA_VERSION,
        "timestamp": time.time(),
        "blocked": blocked,
        "details": details,
    }
    try:
        with open(PROBE_CACHE_PATH, "w", encoding="utf-8") as handle:
            json.dump(payload, handle)
    except OSError:
        pass


def build_probe_project(root: str) -> str:
    os.makedirs(os.path.join(root, "src"), exist_ok=True)

    cargo_toml = os.path.join(root, "Cargo.toml")
    with open(cargo_toml, "w", encoding="utf-8") as handle:
        handle.write(
            "[package]\n"
            'name = "smart_app_control_probe"\n'
            'version = "0.1.0"\n'
            'edition = "2021"\n'
            'build = "build.rs"\n'
        )

    with open(os.path.join(root, "build.rs"), "w", encoding="utf-8") as handle:
        handle.write(
            "fn main() {\n"
            '    println!("cargo:warning=Smart App Control probe build script executed");\n'
            "}\n"
        )

    with open(os.path.join(root, "src", "lib.rs"), "w", encoding="utf-8") as handle:
        handle.write("pub fn probe_ok() -> bool { true }\n")

    return cargo_toml


def probe_output_indicates_block(output: str) -> bool:
    lowered = output.lower()
    patterns = (
        "smart app control",
        "code integrity",
        "verified and reputable",
        "access is denied",
        "this app can't run on your pc",
        "is blocked by your administrator",
    )
    return any(pattern in lowered for pattern in patterns)


def run_build_script_probe() -> tuple[bool | None, str]:
    if os.environ.get("BAIDUPCS_SKIP_SAC_CHECK") == "1":
        trace("Probe skipped via BAIDUPCS_SKIP_SAC_CHECK=1.")
        return False, "Probe skipped via BAIDUPCS_SKIP_SAC_CHECK=1."

    cached, blocked, details = load_probe_cache()
    if cached:
        trace(f"Using cached SAC probe result: blocked={blocked}, details={details!r}")
        return blocked, details

    probe_root = tempfile.mkdtemp(prefix="baidupcs-sac-probe-")
    cargo_toml = build_probe_project(probe_root)
    try:
        env = os.environ.copy()
        env["CARGO_BUILD_RUSTC_WRAPPER"] = ""
        env["RUSTC_WRAPPER"] = ""

        completed = subprocess.run(
            ["cargo", "build", "--quiet", "--manifest-path", cargo_toml],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=env,
            timeout=180,
        )
        output = "\n".join(
            part for part in (completed.stdout.strip(), completed.stderr.strip()) if part
        )
        if completed.returncode == 0:
            details = "Cargo build-script probe succeeded."
            trace(details)
            store_probe_cache(False, details)
            return False, details

        if probe_output_indicates_block(output):
            details = output or "Cargo build-script probe failed with a Smart App Control-like denial."
            trace(f"SAC probe identified a block: {details!r}")
            store_probe_cache(True, details)
            return True, details

        details = output or "Cargo build-script probe failed for a non-diagnostic reason."
        trace(f"SAC probe was inconclusive: {details!r}")
        store_probe_cache(False, details)
        return None, details
    except (OSError, subprocess.SubprocessError) as exc:
        details = str(exc)
        if probe_output_indicates_block(details):
            trace(f"SAC probe exception identified a block: {details!r}")
            store_probe_cache(True, details)
            return True, details
        trace(f"SAC probe exception was inconclusive: {details!r}")
        store_probe_cache(False, details)
        return None, details
    finally:
        shutil.rmtree(probe_root, ignore_errors=True)


def emit_policy_error(details: str | None = None) -> None:
    print(
        "Smart App Control probe confirmed a local Cargo build-script execution block.",
        file=sys.stderr,
    )
    print(
        "Android Rust JNI libraries cannot be rebuilt on this machine until Smart App Control is turned off",
        file=sys.stderr,
    )
    print(
        "or the build is moved to another machine / VM / WSL environment.",
        file=sys.stderr,
    )
    print(
        "Run scripts/check_windows_rust_build_policy.ps1 for a direct environment check.",
        file=sys.stderr,
    )
    if details:
        print("", file=sys.stderr)
        print("Probe details:", file=sys.stderr)
        print(details, file=sys.stderr)


def probe_cli() -> int:
    state = read_smart_app_control_state()
    if state != 1:
        print(
            "Result: VerifiedAndReputablePolicyState is not enforcing (or not present). No Smart App Control block detected."
        )
        return 0

    verdict, details = run_build_script_probe()
    if verdict is True:
        print("Result: Smart App Control is ON and the Cargo build-script probe was blocked.")
        if details:
            print(details)
        return 2

    if verdict is False:
        print("Result: Smart App Control is ON, but the Cargo build-script probe succeeded on this machine.")
        if details:
            print(details)
        return 0

    print("Result: Smart App Control is ON, but the probe was inconclusive. The build will be allowed to continue.")
    if details:
        print(details)
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        return 1

    if sys.argv[1] == "--probe-smart-app-control":
        return probe_cli()

    trace(
        "Wrapper invoked with rustc command={0!r}, state={1!r}, enforced={2!r}".format(
            sys.argv[1:3],
            read_smart_app_control_state(),
            smart_app_control_enforced(),
        )
    )

    if smart_app_control_enforced():
        verdict, details = run_build_script_probe()
        if verdict is True:
            trace(f"Wrapper blocked the build due to probe verdict=True, details={details!r}")
            emit_policy_error(details)
            return 1

    rustc = sys.argv[1]
    args = sys.argv[2:]

    if is_compile_invocation(args) and not has_target(args):
        if not has_linker_override(args):
            args.extend(["-C", f"linker={HOST_LINKER}"])
        args.extend(HOST_NATIVE_FLAGS)

    completed = subprocess.run([rustc, *args])
    return completed.returncode


if __name__ == "__main__":
    raise SystemExit(main())
