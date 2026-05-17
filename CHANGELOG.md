<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# SpecDD IntelliJ Plugin Changelog

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
