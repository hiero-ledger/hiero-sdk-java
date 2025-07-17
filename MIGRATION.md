# Migration Guide: Hedera Java SDK to Hiero Java SDK

## Overview

This document provides a comprehensive guide for migrating from the Hedera Java SDK to the new Hiero Java SDK. The package has been transferred from the Hedera organization to the Hiero organization, and this migration reflects the updated namespace and group ID.

## What's Changing

The Maven group ID and artifact ID are being updated to reflect the new organization ownership:
- **Group ID**: `com.hedera.hashgraph` → `org.hiero`
- **Artifact ID**: `sdk` → `sdk-java`
- **Package namespace**: `com.hedera.hashgraph.sdk` → `org.hiero.sdk`

The functionality, API, features, and codebase remain exactly the same - only the dependency coordinates and import statements need to be updated.

## Migration Steps

### 1. Update Gradle Dependencies

**Before:**

```gradle
dependencies {
    implementation 'com.hedera.hashgraph:sdk:2.67.0'
}
```

**After:**

```gradle
dependencies {
    implementation 'org.hiero:sdk:2.67.0'
}
```

### 2. Update Maven Dependencies

**Before:**

```xml
<dependency>
    <groupId>com.hedera.hashgraph</groupId>
    <artifactId>sdk</artifactId>
    <version>2.67.0</version>
</dependency>
```

**After:**

```xml
<dependency>
    <groupId>org.hiero</groupId>
    <artifactId>sdk</artifactId>
    <version>2.67.0</version>
</dependency>
```

### 3. Update Import Statements

**Before:**

```java
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.Hbar;
```

**After:**

```java
import org.hiero.sdk.Client;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.AccountBalanceQuery;
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.TransferTransaction;
import org.hiero.sdk.Hbar;
```

### 4. Update Exception Handling

**Before:**

```java
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.ReceiptStatusException;
import com.hedera.hashgraph.sdk.MaxQueryPaymentExceededException;

try {
    // SDK operations
} catch (PrecheckStatusException e) {
    // Handle precheck exceptions
} catch (ReceiptStatusException e) {
    // Handle receipt exceptions
}
```

**After:**

```java
import org.hiero.sdk.PrecheckStatusException;
import org.hiero.sdk.ReceiptStatusException;
import org.hiero.sdk.MaxQueryPaymentExceededException;

try {
    // SDK operations
} catch (PrecheckStatusException e) {
    // Handle precheck exceptions
} catch (ReceiptStatusException e) {
    // Handle receipt exceptions
}
```

### 5. Update Configuration Files

#### Gradle Build Files

Update all `build.gradle` or `build.gradle.kts` files:

**Before:**

```gradle
plugins {
    id 'java'
    id 'application'
}

dependencies {
    implementation 'com.hedera.hashgraph:sdk:2.67.0'
    testImplementation 'junit:junit:4.13.2'
}
```

**After:**

```gradle
plugins {
    id 'java'
    id 'application'
}

dependencies {
    implementation 'org.hiero:sdk:2.67.0'
    testImplementation 'junit:junit:4.13.2'
}
```

#### Maven POM Files

Update `pom.xml` files:

**Before:**

```xml
<dependencies>
    <dependency>
        <groupId>com.hedera.hashgraph</groupId>
        <artifactId>sdk</artifactId>
        <version>2.67.0</version>
    </dependency>
</dependencies>
```

**After:**

```xml
<dependencies>
    <dependency>
        <groupId>org.hiero</groupId>
        <artifactId>sdk</artifactId>
        <version>2.67.0</version>
    </dependency>
</dependencies>
```

## Files That Need Updates

### 1. Build Configuration Files

- `build.gradle` / `build.gradle.kts` files
- `pom.xml` files
- `gradle.properties` files (if they reference the old group ID)
- `settings.gradle` files (if they have specific configurations)

### 2. Source Code Files

- All Java files with import statements
- Test files
- Configuration classes
- Utility classes

### 3. Documentation Files

- README files
- API documentation
- Code examples in documentation
- Tutorial files

### 4. CI/CD Configuration Files

- GitHub Actions workflows
- Jenkins pipelines
- GitLab CI files
- Docker files

## Code Examples

### Basic Client Setup

**Before:**

```java
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.PrivateKey;

public class Example {
    public static void main(String[] args) {
        Client client = Client.forTestnet();

        AccountId operatorId = AccountId.fromString("0.0.1234");
        PrivateKey operatorKey = PrivateKey.fromString("...");

        client.setOperator(operatorId, operatorKey);
    }
}
```

**After:**

```java
import org.hiero.sdk.Client;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.PrivateKey;

public class Example {
    public static void main(String[] args) {
        Client client = Client.forTestnet();

        AccountId operatorId = AccountId.fromString("0.0.1234");
        PrivateKey operatorKey = PrivateKey.fromString("...");

        client.setOperator(operatorId, operatorKey);
    }
}
```

### Account Creation

**Before:**

```java
import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Hbar;

AccountCreateTransaction transaction = new AccountCreateTransaction()
    .setKey(PrivateKey.generate().getPublicKey())
    .setInitialBalance(Hbar.fromTinybars(1000))
    .setAccountMemo("Test account");
```

**After:**

```java
import org.hiero.sdk.AccountCreateTransaction;
import org.hiero.sdk.PrivateKey;
import org.hiero.sdk.Hbar;

AccountCreateTransaction transaction = new AccountCreateTransaction()
    .setKey(PrivateKey.generate().getPublicKey())
    .setInitialBalance(Hbar.fromTinybars(1000))
    .setAccountMemo("Test account");
```

### Transfer Transaction

**Before:**

```java
import com.hedera.hashgraph.sdk.TransferTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;

TransferTransaction transferTx = new TransferTransaction()
    .addHbarTransfer(AccountId.fromString("0.0.1234"), Hbar.fromTinybars(-1000))
    .addHbarTransfer(AccountId.fromString("0.0.5678"), Hbar.fromTinybars(1000));
```

**After:**

```java
import org.hiero.sdk.TransferTransaction;
import org.hiero.sdk.AccountId;
import org.hiero.sdk.Hbar;

TransferTransaction transferTx = new TransferTransaction()
    .addHbarTransfer(AccountId.fromString("0.0.1234"), Hbar.fromTinybars(-1000))
    .addHbarTransfer(AccountId.fromString("0.0.5678"), Hbar.fromTinybars(1000));
```

## Breaking Changes

**There are no breaking changes in this migration.** The package name change is purely cosmetic and does not affect:
- API functionality
- Method signatures
- Class structures
- Return types
- Error handling
- Transaction behavior

## Version Compatibility

The new `org.hiero:sdk` artifact maintains full compatibility with the previous `com.hedera.hashgraph:sdk` versions. You can directly replace the dependency coordinates without any code changes beyond the import statements.

## IDE Configuration

### IntelliJ IDEA

1. After updating dependencies, refresh your Gradle/Maven project
2. Use "Optimize Imports" (Ctrl+Alt+O) to clean up unused imports
3. Use "Find and Replace in Path" (Ctrl+Shift+R) for bulk import updates

### Eclipse

1. Refresh your project after updating build files
2. Use "Organize Imports" (Ctrl+Shift+O) to clean up imports
3. Use "Search > File Search" for bulk text replacements

### VS Code

1. Use the Java extension's "Clean Workspace" command
2. Use "Find and Replace" across files for bulk updates

## Common Issues and Solutions

### Issue: Compilation errors after migration

**Solution:** Ensure all import statements have been updated and dependencies are correctly specified in your build files.

### Issue: Tests failing after migration

**Solution:** Check that test files have also been updated with the new imports and that test dependencies are correctly configured.

### Issue: IDE not recognizing new imports

**Solution:** Refresh/reimport your project and clear any caches.

## Support

If you encounter any issues during the migration:
1. Check that all import statements have been updated
2. Verify that your build configuration files are correct
3. Ensure your dependency resolution is working properly
4. Test your application thoroughly

For additional support, create an issue in the [Hiero SDK repository](https://github.com/hiero-ledger/hiero-sdk-java/issues).

## Timeline

- **Effective Date**: The new `org.hiero:sdk` artifact is available immediately
- **Deprecation**: The `com.hedera.hashgraph:sdk` artifact will continue to work but will eventually be deprecated
- **Recommendation**: Migrate as soon as possible to ensure you're using the officially supported artifact

---

**Note**: This migration is part of the broader transition from the Hedera organization to the Hiero organization. The SDK functionality remains unchanged, and this is purely a namespace update to reflect the new organizational structure.
