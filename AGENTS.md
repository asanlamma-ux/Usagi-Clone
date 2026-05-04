# Repository Instructions

## Kotlin Fix Verification

After making a Kotlin fix, verify it without running Gradle, Java, Android SDK tools, Android Studio, `kotlinc`, or installing dependencies.

Use only file inspection and shell text tools such as `rg`, `sed`, and `ls`.

Verify:

1. The edited code matches the intended bug/fix path.
2. All affected call sites were searched with `rg`.
3. Imports, package names, renamed symbols, and function signatures are consistent.
4. No stale references to old names remain.
5. Nullability, coroutine/threading, lifecycle, and error handling are still correct.
6. Android resource IDs, manifest entries, navigation routes, DI bindings, or XML references affected by the change still line up.
7. The fix does not create obvious compile errors from missing imports, wrong types, bad override signatures, or unreachable variables.
8. Report what could not be verified without a real Gradle/CI build.

Useful static checks:

```bash
rg -n "ChangedClass|changedFunction|oldName|newName" /path/to/project
rg -n "TODO|FIXME|!!|lateinit|GlobalScope|runBlocking" /path/to/project/app/src
rg -n "R\\.id\\.|R\\.layout\\.|R\\.string\\.|android:name|nav_graph" /path/to/project/app/src
```

Key phrase: verify by static analysis and call-site tracing only; explicitly state remaining compile/build risk.
