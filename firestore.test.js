const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validContactPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    name: "Elena Vance",
    phoneNumber: "(415) 890-2314",
    normalizedNumber: "4158902314",
    label: "Mobile",
    email: "elena@studio.io",
    organization: "Vance Studio",
    isVip: true,
    avatarColorIndex: 0,
    notes: "VIP Architect",
    importSource: "VCF_FILE",
    createdAt: now,
    updatedAt: now,
  };
}

function validCallLogPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    phoneNumber: "(415) 890-2314",
    contactName: "Elena Vance",
    direction: "INCOMING",
    filterVerdict: "ALLOWED",
    matchedRuleLabel: "Passed All Filters",
    autoReplyDispatched: false,
    durationSeconds: 120,
    timestamp: Date.now(),
    createdAt: now,
    updatedAt: now,
  };
}

function validFilterRulePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    label: "Block 800 Prefix",
    pattern: "1800",
    matchType: "PREFIX",
    action: "BLOCK",
    isEnabled: true,
    blockedCount: 3,
    autoReplyText: "Automated calls are blocked.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create contacts, callLogs, or filterRules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("contacts").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("contacts").doc("c1").set(validContactPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("callLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("filterRules").get());
});

test("Authenticated user: can create, read, list, and update own contact", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("c1");
  await assertSucceeds(ref.set(validContactPayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(ref.update({ isVip: false, updatedAt: new Date() }));
});

test("Authenticated user: cannot read or modify another user's contacts or call logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("contacts").doc("c_bob").set(validContactPayload(BOB_UID));
    await db.collection("users").doc(BOB_UID).collection("callLogs").doc("l_bob").set(validCallLogPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("contacts").doc("c_bob").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("callLogs").doc("l_bob").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("contacts").where("userId", "==", BOB_UID).get()
  );
});

test("Schema validation: rejects ghost fields, spoofed userId, and invalid enums", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badContactRef = aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("c_bad");

  // Spoofed userId
  await assertFails(badContactRef.set({ ...validContactPayload(ALICE_UID), userId: BOB_UID }));

  // Ghost field injection
  await assertFails(badContactRef.set({ ...validContactPayload(ALICE_UID), isAdmin: true }));

  // Invalid CallLog direction enum
  const badLogRef = aliceDb.collection("users").doc(ALICE_UID).collection("callLogs").doc("l_bad");
  await assertFails(badLogRef.set({ ...validCallLogPayload(ALICE_UID), direction: "UNKNOWN_TYPE" }));

  // Invalid FilterRule action enum
  const badRuleRef = aliceDb.collection("users").doc(ALICE_UID).collection("filterRules").doc("r_bad");
  await assertFails(badRuleRef.set({ ...validFilterRulePayload(ALICE_UID), action: "DROP_ALL" }));
});
