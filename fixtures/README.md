# SpecDD Fixtures

This directory contains a small Python invoice project used as realistic sample material for the SpecDD IntelliJ plugin.
It is intentionally lightweight: there is no packaging metadata, dependency management, or generated output.

The fixture has two roles:

- `app.sdd` and `src/invoice_demo/*.sdd` show source-adjacent specs beside plausible Python code.
- `kitchen-sink.sdd` is kept as a compact coverage fixture for the SpecDD language itself. It intentionally includes all
  supported section labels, task states, scenario steps, explicit paths, globs, inline code spans, and `@` symbol
  references in one file.
- `kitchen-sink-invalid.sdd` is the negative fixture. It intentionally violates the `.sdd` language rules so validation,
  warning, highlighting, and recovery behavior can be checked against a dense error sample.

The Python files are not meant to be a production app. They exist so path navigation, symbol-like references,
highlighting, validation, completion, and structure view behavior can be exercised against a coherent local tree.
