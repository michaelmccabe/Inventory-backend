# PR #2: Make Item Name Unique

This document describes the second PR that needs to be created to address the TODO comment in `ItemController.java` line 30: "make name of item unique"

## Branch Information

Branch name: `copilot/make-item-name-unique`
Base: commit `a8cbd6a` (the commit with the TODOs)

## Changes to be included

The following changes have been implemented and committed locally on branch `copilot/make-item-name-unique`:

### 1. Database Migration
**File**: `src/main/resources/db/migration/V3__Add_unique_constraint_to_item_name.sql`
```sql
-- Add unique constraint to item name
ALTER TABLE items ADD CONSTRAINT uk_items_name UNIQUE (name);
```

### 2. Entity Update
**File**: `src/main/java/com/mictech/model/Item.java`
- Added `@Column(unique = true, nullable = false)` annotation to the `name` field
- Added import for `jakarta.persistence.Column`

### 3. Repository Update
**File**: `src/main/java/com/mictech/repository/ItemRepository.java`
- Added `Optional<Item> findByName(String name);` method

### 4. Service Update
**File**: `src/main/java/com/mictech/service/ItemService.java`
- Updated `createItem()` to check for duplicate names before saving
- Updated `updateItem()` to check for duplicate names (excluding current item)
- Throws `RuntimeException` with descriptive message when duplicate name is detected

### 5. Controller Update
**File**: `src/main/java/com/mictech/controller/ItemController.java`
- Removed TODO comment from line 30

### 6. Test Update
**File**: `src/test/java/com/mictech/controller/ItemControllerIntegrationTest.java`
- Added `testCreateItemWithDuplicateName()` test to verify duplicate name rejection

## How to Create PR #2

Since this branch exists locally but cannot be pushed through the automated system, you have two options:

### Option A: Manual Push (if you have access)
```bash
git checkout copilot/make-item-name-unique
git push -u origin copilot/make-item-name-unique
```
Then create a PR from `copilot/make-item-name-unique` to the base branch.

### Option B: Cherry-pick to a new branch
```bash
git checkout -b pr-2-item-name-unique <base-branch>
git cherry-pick abdd509  # The commit hash for "Make item name unique"
git push -u origin pr-2-item-name-unique
```

## Testing

All tests pass with these changes:
- Total tests: 13
- Failures: 0
- Errors: 0
- Skipped: 0

The new test `testCreateItemWithDuplicateName()` specifically validates that attempting to create two items with the same name results in a server error (500 status code).
