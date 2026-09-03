<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Mutable State Leak Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Real interprocedural alias tracking (crossing class/file boundaries
  via PSI method resolution) flagging a real mutation of a value
  returned by a getter that hands back one of its own mutable fields
  directly -- a confirmed, proven-exploited `EI_EXPOSE_REP` shape
  (CWE-374/375), not just a risky getter.

[Unreleased]: https://github.com/GapHunterLabs/mutable-state-leak-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/mutable-state-leak-companion/commits/0.1.0
