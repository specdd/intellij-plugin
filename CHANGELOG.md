<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# SpecDD IntelliJ Plugin Changelog

## [Unreleased]

## [1.0.3] - 2026-06-04

### Fixed

- Align `Tasks` parsing and validation with the SpecDD language grammar by requiring task-marker and task-id separators.
- Report malformed task markers in `Tasks` as task diagnostics instead of falling through as generic text.
- Extract and highlight brace-alternative glob paths such as `./src/{main,test}.sdd`.
- Trim terminal `:` from explicit `@` symbol references while preserving internal symbol colons.
- Report whitespace before `:` in section headers with a specific validation diagnostic.

## [1.0.2] - 2026-05-19

### Added

- Improved symbol references with IntelliJ navigation, find usages, code-side rename discovery, and SDD-side rename
  invocation.
- Warnings for unresolved symbol, file, and glob references across supported SpecDD text contexts.
- Automatic rename and move refactor support for exact SpecDD file path references.
- Color settings page and bundled Default/Darcula color-scheme attributes for SpecDD highlighting.

### Changed

- Aligned language requirements to SpecDD language reference version 1.0.
- Improve inline code and reference handling to reduce false positive warnings.
- Improve highlighting for symbols, inline code spans, section labels, metadata, paths, globs, task states,
  continuation indentation, and key-value syntax.
- Require explicit project-root or spec-relative path prefixes for path resolution.

### Fixed

- Prevent SDD-side rename from shortening qualified symbol references.
- Preserve symbol qualifiers when a referenced symbol is renamed.
- Resolve dotted symbols through IntelliJ symbol/class contributors using both full and final-segment lookups.
- Resolve project-root `/` SpecDD path references from the IntelliJ project root instead of module or content roots.
- Resolve SpecDD file and glob references through IntelliJ VFS for consistent local and remote development behavior.
- Avoid treating URLs and prose-like dependency names as file references unless they use explicit path prefixes.
- Validate duplicate `Scenario` and `Example` sections only when their inline values repeat.
- Report unsupported body text only for sections that do not allow follow-up body content.
- Keep invalid task states highlighted and diagnosed as errors.

## [1.0.1] - 2026-05-19

### Fixed

- Resolve project-root `/` SpecDD path references from the IntelliJ project root instead of module or content roots.
- Resolve SpecDD file and glob references through IntelliJ VFS for consistent local and remote development behavior.

### Added

- Recursive `**` glob support for SpecDD file and glob references.
- Inline backtick code-span highlighting.
- Clickable references for backticked explicit paths and unambiguous IntelliJ symbols.

### Changed

- Use `/` as the only project-root path prefix, alongside `./` and `../` spec-relative prefixes.
- Keep unresolved or ambiguous backticked symbol references warning-free.

## [1.0.0] - 2026-05-17

### Added

- Initial SpecDD `.sdd` file type support for IntelliJ-based IDEs.
- Syntax highlighting for SpecDD sections, section values, key-value lines, comments, tasks, scenario steps, paths,
  symbols, and continuation indentation.
- Structural validation for known section names, missing separators, required inline values, duplicate sections,
  duplicate scenario names, unsupported section body text, invalid task states, and indentation rules.
- Structure view entries for sections in SpecDD files.
- Hover documentation for known SpecDD sections.
- Completion for section names, local symbols, project files, relative paths, and parent directory paths.
- Clickable navigation for existing file and glob references in SpecDD files.
- Warnings for unresolved path and glob references.
- Create-file quick fix for unresolved exact file references inside the project.
- Default SpecDD code style using 2-space indentation and spaces instead of tabs.
- Plugin metadata, icon, file type icon, and production build target.
