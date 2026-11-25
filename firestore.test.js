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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function buildValidVideoPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 5000);
  return {
    userId,
    title: "Neon Night Shoot",
    caption: "Testing the Cyber filter in ReelPulse camera studio #night",
    category: "Tech",
    tags: ["#tech", "#reelpulse"],
    soundTitle: "Original Camera Sound — @alice",
    videoUri: "file:///data/user/0/com.aistudio.reelpulse.vq8zkm/files/recordings/reel_1.mp4",
    filterId: "cyber_pulse",
    filterName: "Cyber",
    sourceType: "camera",
    durationMs: 14500,
    creatorName: "Alice Rivera",
    creatorHandle: "@alice.rivera",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write /users/{userId}", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      handle: "@hacker",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("videos").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("videos")
      .doc("vid_unauth")
      .set(buildValidVideoPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin create and partial update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Mirrors UserAccountRepository.ensureOrUpsertAccount create payload (no @DocumentId `id`)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Rivera",
      handle: "@alice.rivera",
      email: "alice@example.com",
      mobileNumber: "+1 555 234 5678",
      bio: "Filming neon nights.",
      accountVisibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Mirrors UserAccountRepository.updateAccountProfile partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice R.",
      handle: "@alice.r",
      mobileNumber: "+1 555 987 6543",
      bio: "Creator on ReelPulse",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Mirrors UserAccountRepository.updateAccountVisibility toggle to personal
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountVisibility: "personal",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Rejects invalid accountVisibility value
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      accountVisibility: "friends_only",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Rejects ghost fields, @DocumentId id key, future timestamps, and immutable createdAt tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Reject unexpected @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field like isAdmin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Create valid doc then reject createdAt mutation
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      handle: "@alice",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's profile or videos", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      handle: "@bob",
      email: "bob@example.com",
      mobileNumber: "+1 555 111 2222",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("videos")
      .doc("bob_vid_1")
      .set(buildValidVideoPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("users").get());

  // Cross-user isolation on /users/{bob}/videos
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("videos").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("videos")
      .doc("bob_vid_1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("videos")
      .doc("alice_injected")
      .set(buildValidVideoPayload(ALICE_UID))
  );
});

test("Video metadata: owner create, ordered list query, partial update succeed and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const videosRef = aliceDb.collection("users").doc(ALICE_UID).collection("videos");

  // Empty subcollection list succeeds
  await assertSucceeds(videosRef.orderBy("createdAt", "desc").get());

  // Valid create (mirrors VideoMetadataRepository.saveVideoMetadata)
  await assertSucceeds(
    videosRef.doc("vid_1732500001_ab12").set(buildValidVideoPayload(ALICE_UID))
  );

  // Ordered list query matching Kotlin repository succeeds
  await assertSucceeds(videosRef.orderBy("createdAt", "desc").get());

  // Valid partial update
  await assertSucceeds(
    videosRef.doc("vid_1732500001_ab12").update({
      caption: "Updated caption with #cyber filter",
      filterId: "noir_cinema",
      filterName: "Noir",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & @DocumentId `id`
  await assertFails(
    videosRef.doc("vid_ghost").set(buildValidVideoPayload(ALICE_UID, { id: "vid_ghost" }))
  );
  await assertFails(
    videosRef.doc("vid_admin").set(buildValidVideoPayload(ALICE_UID, { featured: true }))
  );

  // Reject invalid sourceType
  await assertFails(
    videosRef.doc("vid_bad_source").set(buildValidVideoPayload(ALICE_UID, { sourceType: "external" }))
  );

  // Reject empty caption
  await assertFails(
    videosRef.doc("vid_empty_caption").set(buildValidVideoPayload(ALICE_UID, { caption: "" }))
  );

  // Reject immutable createdAt / videoUri tamper on update
  await assertFails(
    videosRef.doc("vid_1732500001_ab12").update({
      videoUri: "https://evil.example/video.mp4",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Live streams & live chat: public discovery, personal isolation, chat, reactions, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 5000);

  // Unauthenticated cannot list or create live streams
  await assertFails(
    unauthDb.collection("live_streams").where("visibility", "==", "public").get()
  );

  // Alice starts a public live stream
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("live_alice_1").set({
      hostUserId: ALICE_UID,
      hostName: "Alice Rivera",
      hostHandle: "@alice.rivera",
      title: "Midnight Beat Session",
      category: "Music",
      filterId: "cyber_pulse",
      status: "live",
      visibility: "public",
      viewerCount: 1,
      likeCount: 0,
      startedAt: now,
      updatedAt: now,
    })
  );

  // Filtered query by visibility == "public" and status == "live" succeeds
  await assertSucceeds(
    bobDb
      .collection("live_streams")
      .where("visibility", "==", "public")
      .where("status", "==", "live")
      .get()
  );

  // Unfiltered list on live_streams fails
  await assertFails(bobDb.collection("live_streams").get());

  // Bob can read public live chat (even when empty) and send a chat message + heart reaction
  await assertSucceeds(
    bobDb
      .collection("live_streams")
      .doc("live_alice_1")
      .collection("chat")
      .where("streamVisibility", "==", "public")
      .orderBy("createdAt", "asc")
      .get()
  );

  await assertSucceeds(
    bobDb
      .collection("live_streams")
      .doc("live_alice_1")
      .collection("chat")
      .doc("msg_1")
      .set({
        streamId: "live_alice_1",
        streamHostId: ALICE_UID,
        streamVisibility: "public",
        senderId: BOB_UID,
        senderName: "Bob",
        senderHandle: "@bob",
        text: "Loving this synth groove!",
        createdAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(
    bobDb.collection("live_streams").doc("live_alice_1").update({
      likeCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot end Alice's stream or spoof senderId in chat
  await assertFails(
    bobDb.collection("live_streams").doc("live_alice_1").update({
      status: "ended",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertFails(
    bobDb
      .collection("live_streams")
      .doc("live_alice_1")
      .collection("chat")
      .doc("msg_spoof")
      .set({
        streamId: "live_alice_1",
        streamHostId: ALICE_UID,
        streamVisibility: "public",
        senderId: ALICE_UID,
        senderName: "Alice",
        senderHandle: "@alice.rivera",
        text: "Spoofed message",
        createdAt: new Date(Date.now() - 300),
      })
  );

  // Alice starts a personal live stream — Bob cannot read or list it
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("live_alice_private").set({
      hostUserId: ALICE_UID,
      hostName: "Alice Rivera",
      hostHandle: "@alice.rivera",
      title: "Private Rehearsal",
      category: "Music",
      filterId: "normal",
      status: "live",
      visibility: "personal",
      viewerCount: 1,
      likeCount: 0,
      startedAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("live_streams").doc("live_alice_private").get());
  await assertSucceeds(
    aliceDb.collection("live_streams").where("hostUserId", "==", ALICE_UID).get()
  );

  // Alice ends live_alice_1; terminal state lock prevents re-opening or new chat messages
  await assertSucceeds(
    aliceDb.collection("live_streams").doc("live_alice_1").update({
      status: "ended",
      endedAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    aliceDb.collection("live_streams").doc("live_alice_1").update({
      status: "live",
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    bobDb
      .collection("live_streams")
      .doc("live_alice_1")
      .collection("chat")
      .doc("msg_after_end")
      .set({
        streamId: "live_alice_1",
        streamHostId: ALICE_UID,
        streamVisibility: "public",
        senderId: BOB_UID,
        senderName: "Bob",
        senderHandle: "@bob",
        text: "Too late",
        createdAt: new Date(Date.now() - 100),
      })
  );
});

test("Blocked users: owner create/list/delete succeed; cross-user, self-block, update, and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  const aliceBlockedRef = aliceDb.collection("users").doc(ALICE_UID).collection("blocked_users");

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("blocked_users").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("blocked_users")
      .doc("creator_kai")
      .set({
        userId: ALICE_UID,
        blockedCreatorId: "creator_kai",
        blockedCreatorName: "Kai Mercer",
        blockedCreatorHandle: "@kai.synths",
        blockedCreatorAvatarKey: "tech",
        createdAt: now,
      })
  );

  // Empty list query succeeds for owner
  await assertSucceeds(aliceBlockedRef.orderBy("createdAt", "desc").get());

  // Valid create succeeds
  await assertSucceeds(
    aliceBlockedRef.doc("creator_kai").set({
      userId: ALICE_UID,
      blockedCreatorId: "creator_kai",
      blockedCreatorName: "Kai Mercer",
      blockedCreatorHandle: "@kai.synths",
      blockedCreatorAvatarKey: "tech",
      createdAt: now,
    })
  );

  // Ordered list query matching BlockedUserRepository succeeds
  await assertSucceeds(aliceBlockedRef.orderBy("createdAt", "desc").get());

  // Reject self-blocking and local_user blocking
  await assertFails(
    aliceBlockedRef.doc(ALICE_UID).set({
      userId: ALICE_UID,
      blockedCreatorId: ALICE_UID,
      blockedCreatorName: "Alice",
      blockedCreatorHandle: "@alice",
      blockedCreatorAvatarKey: "local_user",
      createdAt: now,
    })
  );
  await assertFails(
    aliceBlockedRef.doc("local_user").set({
      userId: ALICE_UID,
      blockedCreatorId: "local_user",
      blockedCreatorName: "Alice",
      blockedCreatorHandle: "@alice",
      blockedCreatorAvatarKey: "local_user",
      createdAt: now,
    })
  );

  // Reject ghost field and @DocumentId `id`
  await assertFails(
    aliceBlockedRef.doc("creator_maya").set({
      id: "creator_maya",
      userId: ALICE_UID,
      blockedCreatorId: "creator_maya",
      blockedCreatorName: "Maya Lin",
      blockedCreatorHandle: "@maya.moves",
      blockedCreatorAvatarKey: "dancer",
      createdAt: now,
    })
  );

  // Reject updates (immutable block record; unblock via delete)
  await assertFails(
    aliceBlockedRef.doc("creator_kai").update({
      blockedCreatorName: "Tampered",
    })
  );

  // Cross-user isolation: Bob cannot read, write, or unblock Alice's blocked_users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("blocked_users").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("blocked_users").doc("creator_kai").delete()
  );

  // Owner can unblock (delete)
  await assertSucceeds(aliceBlockedRef.doc("creator_kai").delete());
});
