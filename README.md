## Plugin Fixtures

This is a library with collection of test fixtures to help plugin authors test common plugin scenarios.

Note: this is an experimental project, it does not mean this will land anywhere at any point

### Getting Started
// TODO

### Well-behaved plugin test

Currently, this library consist of one fixture to test well-behaved plugin. You can test this by extending `AbstractWellBehavedPluginTest` and implementing abstract methods.

By implementing `AbstractWellBehavedPluginTest` you will automatically test:
- Task registration and other tasks configuration avoidance
- Build cache & relocatability
- Up-to-date checks (out of date tasks)