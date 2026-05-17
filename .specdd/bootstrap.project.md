# SpecDD project specific overrides

This is an IntelliJ plugin that adds support for the SpecDD language itself.

The root `app.sdd` file is the repository-level SpecDD contract for this project.

The root of source code is in `src/` directory of the repository root.

## Code style

1. Use Yoda conditions for comparisons, where applicable.
2. Follow object-oriented programming principles.
3. Enforce strong API boundaries. Do not expose internal implementation details unnecessarily.
4. Separate features, concerns, and related units into dedicated units of code.
5. Follow the single responsibility principle.
6. Prefer early returns to reduce nesting and improve control flow clarity.
7. Apply logging generously, using appropriate log levels for the context.
8. Use curly braces for all blocks.
