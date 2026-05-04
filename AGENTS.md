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

## Manifest And Integration Deep Fixes

Before starting a deep fix workflow, ask whether the user will actively oversee the process or wants autonomous progress. If the user already gave an explicit execution instruction, proceed while keeping updates concise.

For any edit that touches `AndroidManifest.xml`, Gradle dependency declarations, Android permissions, exported components, package visibility, foreground services, file/storage access, account/sync integration, navigation/deep links, app widgets, notifications, or feature integrations that might require manifest entries or new dependencies:

1. Inspect what the manifest or dependency currently declares.
2. Trace what the app actually uses with `rg`: permissions, APIs, component classes, services, receivers, providers, activities, intent actions, metadata, resource references, DI bindings, and call sites.
3. Compare declarations against usage. Remove broad or stale entries when a narrower declaration covers the real use case.
4. Check whether each relevant API, manifest attribute, permission, dependency version, or Gradle DSL is deprecated, replaced, restricted, or target-SDK-sensitive.
5. When the behavior is current-policy or version dependent, verify against current official documentation, release notes, or dependency changelogs before choosing the fix. Prefer official Android, AndroidX, Gradle, and library sources.
6. For new dependencies, confirm the app really needs them, check their minSdk/targetSdk impact, manifest merge impact, transitive permissions, package size/native library effects, and whether an existing dependency already provides the needed API.
7. For new manifest entries, confirm the matching code path exists and the entry is the narrowest correct declaration. For removed entries, confirm no code still depends on them.
8. Verify by static analysis and call-site tracing before finishing. Do not assume a manifest edit is correct just because it looks syntactically valid.

### Autonomy For Safe Deprecation Fixes

When the issue is a known deprecated manifest attribute, permission, Gradle DSL, or dependency setting and the replacement is documented, behavior-preserving, narrow, and low risk, fix it without asking for extra confirmation if the user has already asked for a fix or is overseeing the pass.

Examples of safe autonomous fixes:

1. Move a deprecated manifest attribute to the documented Gradle DSL without changing runtime intent.
2. Replace a broad package visibility declaration with a narrower `<queries>` entry that exactly matches existing `PackageManager` calls.
3. Add a missing permission declaration only when code already requests or checks that permission and the SDK cap is clear.
4. Correct a stale manifest class or metadata reference when the real class is unambiguous in source.

Do not ask for confirmation just to make a safe, documented replacement. Research the replacement, apply the narrow fix, verify by static analysis, and explain the change in the final output.

Ask for confirmation before changes that alter app policy, user-visible security/privacy behavior, compatibility, or distribution risk. Examples:

1. Raising `minSdk`, dropping device support, or changing target SDK behavior.
2. Adding broad or sensitive permissions such as `QUERY_ALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, accessibility, SMS, contacts, location, camera, microphone, install/delete package permissions, or notification listener access.
3. Making a component exported, adding broad deep links, weakening network security, changing backup exposure, or relaxing file-provider grants.
4. Using `tools:overrideLibrary` or other manifest overrides that bypass declared compatibility.
5. Replacing a dependency with an uncertain API surface or one that changes transitive manifest permissions.

If the user explicitly asks to review or relay sensitive manifest fixes before editing, do the research and present the safe alternatives first. Otherwise, for low-risk documented replacements, make the fix and report what changed.

Useful manifest/integration checks:

```bash
rg -n "android:name|uses-permission|queries|provider|service|receiver|activity|meta-data|foregroundServiceType" /path/to/project/app/src/main/AndroidManifest.xml
rg -n "ChangedPermission|ChangedService|ChangedActivity|ChangedReceiver|ChangedProvider|ChangedAction|ChangedDependency" /path/to/project/app/src /path/to/project/app/build.gradle /path/to/project/gradle
rg -n "extractNativeLibs|QUERY_ALL_PACKAGES|requestLegacyExternalStorage|FOREGROUND_SERVICE|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|POST_NOTIFICATIONS" /path/to/project/app/src /path/to/project/app/build.gradle
```

## Work Boundaries

Do not blindly agree to a requested change when it is unsafe, impossible, deprecated, policy-incompatible, or unverifiable in the current environment. State the constraint clearly, propose the safest workable path, and ask for confirmation when external verification or a product decision is required.

If context limits or missing external access prevent a reliable finish, make a safe cut: stop before risky edits, summarize what is known, identify the exact unresolved risk, and ask the user for the next action or the needed verification source.

Before ending a pass, report:

1. What was changed.
2. What was verified locally by static inspection.
3. What could not be verified without Gradle, CI, Android SDK tooling, device testing, credentials, or external review.

## GitHub Automation, Push, And CI Logs

When local commits need to be pushed to GitHub from this workspace, use the PAT-backed helper instead of raw `git push` if authentication is required:

```bash
python3 /root/github_commit_push.py -C "/root/Usagi Comic Reader/Usagi" --push-only --branch main
```

When committing new local edits and pushing them in one pass, use:

```bash
python3 /root/github_commit_push.py -C "/root/Usagi Comic Reader/Usagi" --all -m "Commit message" --branch main
```

This helper shares private config with `/root/github_ci_logs.py`, does not print the token, and prints the follow-up CI trace commands after a successful push. Do not paste tokens into remotes, command output, commits, logs, or final responses.

For GitHub Actions failures, use the CI log helper rather than asking the user to manually upload logs when credentials are already available:

```bash
/root/github_ci_logs.py commit-runs HEAD
/root/github_ci_logs.py commit-logs HEAD
```

For a specific pushed commit, replace `HEAD` with the full commit SHA printed by `/root/github_commit_push.py`. Downloaded logs are saved under `/root/github-ci-logs` unless another output directory is supplied. After reading CI logs, summarize the failing workflow, job, task, and the smallest source-level fix path.

## Feature And Logic Regression Guard

When implementing or fixing any feature, do not only verify the edited line. Trace the feature's surrounding logic and likely callers so the change does not fix one issue while breaking adjacent behavior.

Before editing:

1. Identify the feature path: entry point, state/model objects, side effects, persistence, UI binding, background work, network/storage calls, and error/cancellation behavior.
2. Use `rg` to find all direct call sites, indirect references, resource keys, preference keys, intent actions, worker names, DI bindings, and tests or fixtures related to the touched code.
3. Read enough neighboring code to understand invariants and existing behavior before changing it.

After editing:

1. Re-trace affected call sites and compare old behavior to new behavior.
2. Mentally simulate the main flow, empty/null flow, failure flow, cancellation flow, retry flow, and lifecycle/background flow when relevant.
3. Check whether the change affects persistence compatibility, resource IDs, preference keys, database fields, serialization names, intent extras, notification actions, or worker IDs.
4. Check whether concurrency, coroutine context, cancellation propagation, locks, channels, flows, observers, and lifecycle ownership still make sense.
5. Check whether error handling still preserves cleanup, temp-file deletion, state updates, analytics, logs, notifications, and user-visible status.
6. Search for stale names and partially migrated concepts so old and new logic are not both active accidentally.
7. Prefer narrow fixes over broad rewrites unless the surrounding architecture clearly requires a larger change.

Useful feature/regression checks:

```bash
rg -n "ChangedFeature|ChangedClass|changedFunction|preference_key|intent_action|worker_name" /path/to/project/app/src
rg -n "oldName|newName|deprecatedPath|legacyPath|TODO|FIXME|!!|runBlocking|GlobalScope" /path/to/project/app/src
rg -n "emit\\(|collect\\(|launch\\(|async\\(|withContext\\(|synchronized\\(|Mutex|Channel|Flow|StateFlow|LiveData" /path/to/project/app/src/main/kotlin
```

Before finishing, explicitly state the flows that were reasoned through and the flows that still require real runtime, CI, or device verification.

## Opportunistic Bug Fixes

When working in an area and you encounter a clear bug or very likely bug that the user did not explicitly mention, fix it only when all of these are true:

1. The bug is in or directly adjacent to the files/flows already being touched.
2. The fix is safe, narrow, behavior-preserving, and consistent with existing architecture.
3. The root cause can be explained from source inspection or logs, not speculation.
4. The fix can be verified by the allowed verification method for the current task.
5. The change does not require a product decision, sensitive permission/policy change, broad dependency change, or unrelated refactor.

Examples of acceptable opportunistic fixes:

1. Stale class, resource, preference, or manifest references that clearly point to renamed existing symbols.
2. Missing resource declarations for already-used IDs.
3. Obvious cleanup leaks such as temp files not deleted on an adjacent failure path.
4. Nullability or cancellation handling bugs directly exposed by the touched flow.
5. Deprecated API replacements that are documented and behavior-preserving.

Do not expand the scope into speculative improvements. If the likely bug is important but not safe to fix immediately, mention it in the final output as a follow-up with the evidence found.

If a suspected bug cannot be explained confidently from source inspection, logs, or documented behavior, do not patch it as fact. Address it to the user as a hypothesis, explain the signal that made it suspicious, and give a concrete thing to test in the next build or local run. Keep this separate from confirmed fixes.

For every opportunistic fix, verify it with call-site tracing and include it in the final output under a separate note so the user can distinguish requested work from extra safety fixes.
