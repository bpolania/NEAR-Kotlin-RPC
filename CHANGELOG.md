# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 (2025-09-12)


### Features

* implement NEAR Kotlin RPC client ([6dd314c](https://github.com/bpolania/NEAR-Kotlin-RPC/commit/6dd314cd4a51e65b13c85085b5033579e380ed6b))


### Bug Fixes

* improve CI pipeline reliability and debugging ([6c16b21](https://github.com/bpolania/NEAR-Kotlin-RPC/commit/6c16b218c0d1e42f78dbec781e4c19dbfbfd5f0b))
* update CI pipeline to use JDK 17 and fix JaCoCo configuration ([ba28716](https://github.com/bpolania/NEAR-Kotlin-RPC/commit/ba28716dee4dd76e4ae15ba3396a74d79a19a0e5))

## [Unreleased]

### Added
- Initial implementation of NEAR Kotlin RPC client
- Auto-generation from OpenAPI specification
- Two packages: near-jsonrpc-types and near-jsonrpc-client
- GitHub Actions CI/CD automation
- Release-please integration for automated releases
- Daily OpenAPI spec checking and PR generation
- Comprehensive test suite and examples
- Support for all NEAR JSON-RPC methods
- Snake_case to camelCase conversion
- Post-processing fixes for generated code
