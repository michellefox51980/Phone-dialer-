# ClarityDial Firestore Security Specification

## 1. Data Invariants
- All user data lives strictly inside per-user subcollections under `/users/{userId}/{collection}/{docId}`.
- Unauthenticated callers (`request.auth == null`) are denied all reads and writes across the entire database.
- An authenticated user (`request.auth.uid`) can only read, list, create, update, or delete documents where the path `{userId}` equals `request.auth.uid` AND `resource.data.userId == request.auth.uid` (or `request.resource.data.userId == request.auth.uid` on writes).
- Every `create` and `update` enforces `isValid[Entity](request.resource.data)`, strict key allowlists (`hasOnly` and `hasAll`), bounded string lengths, immutable `userId` and `createdAt` on updates, and server timestamps (`<= request.time`).

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `GET /users/alice/contacts/c1` with `auth = null` -> Reject.
2. **Cross-User PII Read**: `GET /users/bob/contacts/c1` as `alice` -> Reject.
3. **Cross-User Call Log List**: `LIST /users/bob/callLogs` as `alice` -> Reject.
4. **Identity Spoofing on Create**: `CREATE /users/alice/contacts/c1` with `userId: "bob"` -> Reject.
5. **Ghost Field Shadow Update**: `UPDATE /users/alice/contacts/c1` adding `isAdmin: true` -> Reject via `hasOnly`.
6. **Immutable Field Mutation**: `UPDATE /users/alice/contacts/c1` modifying `createdAt` or `userId` -> Reject.
7. **Oversized String DoW**: `CREATE /users/alice/contacts/c1` with `name` of 5,000 chars -> Reject via `.size() <= 120`.
8. **Invalid Enum Value**: `CREATE /users/alice/callLogs/l1` with `direction: "HACKED"` -> Reject.
9. **Future Timestamp Spoofing**: `CREATE /users/alice/callLogs/l1` with `createdAt` in future beyond `request.time` -> Reject.
10. **Invalid Rule Action**: `CREATE /users/alice/filterRules/r1` with `action: "DESTROY"` -> Reject.
11. **Malformed Document ID**: `CREATE /users/alice/contacts/bad$id` -> Reject via `isValidId`.
12. **Type Poisoning on Update**: `UPDATE /users/alice/filterRules/r1` setting `blockedCount: "many"` -> Reject via `isValidFilterRule`.
