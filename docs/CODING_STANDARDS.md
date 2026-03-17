# ETHIOSTAT Coding Standards & Documentation Guidelines

## 📁 Directory Structure & Naming Conventions

### Directory Naming
- Use **lowercase** with **hyphens** for multi-word directories
- Examples: `user-profile`, `sms-parser`, `data-models`
- Avoid underscores or camelCase in directory names

### File Naming Conventions

#### Kotlin/Java Classes
- **PascalCase** for class names: `SmsParserService`, `UserProfileActivity`
- **camelCase** for file names when not classes: `utilityFunctions.kt`
- **Interface naming**: Prefix with `I` → `IDataRepository`
- **Abstract classes**: Prefix with `Abstract` → `AbstractBaseActivity`

#### Resource Files
- **snake_case** for all resource files
- Layout files: `activity_main.xml`, `fragment_dashboard.xml`
- Drawable files: `ic_arrow_back.xml`, `bg_rounded_corner.xml`
- String resources: Group by feature → `strings_auth.xml`, `strings_dashboard.xml`

#### Test Files
- Same name as class being tested + `Test` suffix
- Unit tests: `SmsParserServiceTest.kt`
- Integration tests: `DatabaseIntegrationTest.kt`
- UI tests: `MainActivityUITest.kt`

## 📚 Documentation Standards

### Documentation File Placement
**ALL documentation files MUST be placed in the `docs/` directory**

**Exception**: `README.md` may remain in the root directory but should be brief and primarily link to detailed documentation in `docs/`. For comprehensive project documentation, use `docs/PROJECT_OVERVIEW.md`.

### Documentation File Naming
- Use **UPPERCASE** with **underscores** for major documentation
- Examples: `DEPLOYMENT_GUIDE.md`, `API_REFERENCE.md`, `TROUBLESHOOTING.md`
- Feature-specific docs: `SMS_PARSING.md`, `USER_AUTHENTICATION.md`

### Required Documentation Files
1. **PROJECT_DESCRIPTION.md** - Project overview and goals
2. **DEPLOYMENT_GUIDE.md** - How to build and deploy
3. **IMPLEMENTATION_SUMMARY.md** - Technical implementation details
4. **API_REFERENCE.md** - API endpoints and usage
5. **TROUBLESHOOTING.md** - Common issues and solutions
6. **CONFIGURATION_MANAGEMENT.md** - Configuration setup
7. **MULTILINGUAL_GUIDE.md** - Internationalization guide
8. **MVI_ARCHITECTURE.md** - Architecture patterns used

### Documentation Structure Template
```markdown
# [TITLE]

## Overview
Brief description of what this document covers.

## Prerequisites
What the reader needs to know/have before proceeding.

## Step-by-Step Instructions
Detailed, numbered steps with code examples.

## Troubleshooting
Common issues and their solutions.

## References
Links to related documentation or external resources.
```

## 🏗️ Code Organization Rules

### Package Structure
```
com.ethiostat.app/
├── data/           # Data layer (repositories, models, database)
├── domain/         # Business logic (use cases, entities)
├── presentation/   # UI layer (activities, fragments, viewmodels)
├── di/            # Dependency injection modules
├── utils/         # Utility classes and extensions
└── constants/     # Application constants
```

### Class Naming Patterns
- **Activities**: `[Feature]Activity` → `DashboardActivity`
- **Fragments**: `[Feature]Fragment` → `BalanceFragment`
- **ViewModels**: `[Feature]ViewModel` → `DashboardViewModel`
- **Repositories**: `[Entity]Repository` → `UserRepository`
- **Services**: `[Purpose]Service` → `SmsParserService`
- **Adapters**: `[Entity]Adapter` → `TransactionAdapter`
- **Utils**: `[Purpose]Utils` → `DateUtils`, `ValidationUtils`

### Method Naming
- Use **camelCase** for all methods
- Be descriptive: `parseIncomingSmsMessage()` not `parse()`
- Boolean methods: Start with `is`, `has`, `can` → `isValidPhoneNumber()`
- Event handlers: Start with `on` → `onSmsReceived()`

## 📝 Code Documentation Rules

### Class Documentation
```kotlin
/**
 * Parses incoming SMS messages to extract balance information.
 * 
 * This service handles SMS messages from various Ethiopian banks and telecom
 * providers to extract account balance and transaction details.
 * 
 * @author ETHIOSTAT Team
 * @since 1.0.0
 */
class SmsParserService {
    // Implementation
}
```

### Method Documentation
```kotlin
/**
 * Parses an SMS message to extract balance information.
 * 
 * @param message The raw SMS message content
 * @param sender The sender's phone number or service code
 * @return ParsedBalance object containing extracted data, or null if parsing fails
 * @throws IllegalArgumentException if message is empty or null
 */
fun parseBalance(message: String, sender: String): ParsedBalance?
```

### Inline Comments
- Use `//` for single-line explanations
- Use `/* */` for multi-line explanations
- Explain **WHY**, not **WHAT**
- Comment complex business logic and workarounds

## 🚫 Prohibited Practices

### File Placement
- **NEVER** place documentation files in the root directory
- **NEVER** mix documentation with source code directories
- **NEVER** use spaces in file or directory names

### Naming Violations
- **NEVER** use abbreviations in class names: `UsrPrfl` ❌ → `UserProfile` ✅
- **NEVER** use numbers in class names unless absolutely necessary
- **NEVER** use special characters except hyphens and underscores where specified

### Documentation Violations
- **NEVER** leave classes or public methods undocumented
- **NEVER** write documentation that just repeats the code
- **NEVER** use TODO comments in production code without JIRA tickets

## 🔧 Enforcement

### Pre-commit Hooks
- Lint checks for naming conventions
- Documentation coverage checks
- File placement validation

### Code Review Checklist
- [ ] All new files follow naming conventions
- [ ] Documentation is in correct location
- [ ] Classes and public methods are documented
- [ ] Directory structure is maintained

### IDE Configuration
- Configure IDE templates for consistent file headers
- Set up code style formatting rules
- Enable real-time linting for naming conventions

---

**Last Updated**: March 2026  
**Version**: 1.0  
**Maintainer**: ETHIOSTAT Development Team
