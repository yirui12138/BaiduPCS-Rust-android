import os
import subprocess
import sys


HOST_LINKER = os.environ.get("BAIDUPCS_HOST_LINKER", "")


def native_flags_from_env() -> list[str]:
    raw_flags = os.environ.get("BAIDUPCS_HOST_NATIVE_FLAGS", "")
    return [flag.strip() for flag in raw_flags.split(os.pathsep) if flag.strip()]


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


def main() -> int:
    if len(sys.argv) < 2:
        return 1

    rustc = sys.argv[1]
    args = sys.argv[2:]
    host_native_flags = native_flags_from_env()

    if is_compile_invocation(args) and not has_target(args):
        if HOST_LINKER and not has_linker_override(args):
            args.extend(["-C", f"linker={HOST_LINKER}"])
        args.extend(host_native_flags)

    completed = subprocess.run([rustc, *args])
    return completed.returncode


if __name__ == "__main__":
    raise SystemExit(main())
