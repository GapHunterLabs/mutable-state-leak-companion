# Mutable State Leak Companion

Flags a real mutation of a value returned by a getter that hands back
one of its own mutable fields directly.

## Why it exists

The real `EI_EXPOSE_REP` bug class (SpotBugs' own name for it) --
CWE-374/375. SpotBugs is bytecode-based, a CI/build-step tool, not an
inline IDE inspection, and its own heuristic only checks the getter's
SHAPE -- it never verifies a caller actually mutates the returned
reference. No dedicated Marketplace plugin found that verifies real
exploitation.

## Why built this way

- **Real interprocedural alias tracking, not just a getter-shape
  heuristic** -- a local variable assigned from `obj.getXxx()` is
  tracked as an ALIAS of the internal field, and only flagged once a
  REAL mutation of that alias is found in the same method. SpotBugs'
  own check stops at "the getter looks risky"; this plugin requires
  proof the risk is actually realized.
- **Crosses file/class boundaries for free** -- `getXxx()` can live in
  any other class in the project. Real PSI method resolution
  (`PsiMethodCallExpression.resolveMethod()`) naturally reaches into
  that other class's own method body -- no custom whole-project graph
  or cache needed for this one-hop shape.

## v0.1 scope — stated honestly, not exhaustively

- Only a getter whose ENTIRE body is exactly
  `return field;`/`return this.field;` -- a getter that wraps the
  field in any other expression (even a harmless one) is out of scope.
- Only `List`/`Map`/`Set` (and their common implementations) or an
  array field type.
- Only tracks the mutation within the SAME method as the getter call --
  passing the alias further to another method before mutating it is
  out of scope.

## Usage

Open a Java method that calls another class's getter, assigns the
result to a local variable, and then mutates that variable -- the
mutation call site shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
