const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

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

test("Unauthenticated access is denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("lectures").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("grades").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get());
});

test("Student profile: owner create, partial update, shadow field and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Jaspher Omingo",
      regNumber: "IN13/00142/23",
      programme: "BSc. Computer Science",
      semesterLabel: "Sem 1 2026/2027",
      campus: "Main Campus",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      semesterLabel: "Sem 2 2026/2027",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      campus: "Town Campus",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Lectures subcollection: create, timetable update, @DocumentId rejection, and list isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Empty list query succeeds for owner and fails for another user
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("lectures").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("lectures").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lectures").doc("lec_comp314").set({
      userId: ALICE_UID,
      courseCode: "COMP 314",
      courseTitle: "Distributed Systems",
      lecturer: "Dr. M. Nyabuto",
      dayOfWeek: "TUESDAY",
      startTime: "08:00",
      endTime: "10:00",
      venueName: "Tuition Block A - TBA 204",
      venueLat: -0.69235,
      venueLng: 34.7847,
      geofenceRadiusMeters: 120,
      status: "SCHEDULED",
      updateNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Timetable status & venue update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lectures").doc("lec_comp314").update({
      status: "VENUE_CHANGED",
      venueName: "Science Complex - SC 101",
      venueLat: -0.6919,
      venueLng: 34.7851,
      updateNote: "Moved due to lab maintenance",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lectures").doc("lec_comp314").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject forbidden `id` property (@DocumentId trap guard)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("lectures").doc("lec_extra").set({
      id: "lec_extra",
      userId: ALICE_UID,
      courseCode: "COMP 321",
      courseTitle: "Compiler Construction",
      lecturer: "Prof. K. Moraa",
      dayOfWeek: "WEDNESDAY",
      startTime: "11:00",
      endTime: "13:00",
      venueName: "ICT Lab 1",
      venueLat: -0.6921,
      venueLng: 34.7844,
      geofenceRadiusMeters: 100,
      status: "SCHEDULED",
      updateNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Grades subcollection: create, update, and invalid bounds rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("grades").doc("grade_comp314").set({
      userId: ALICE_UID,
      courseCode: "COMP 314",
      courseTitle: "Distributed Systems",
      semester: "Sem 1 2026/2027",
      credits: 3,
      catScore: 24.5,
      assignmentScore: 17.0,
      examScore: 46.0,
      totalScore: 87.5,
      gradeLetter: "A",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("grades").doc("grade_comp314").update({
      examScore: 48.0,
      totalScore: 89.5,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Invalid grade letter rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("grades").doc("grade_bad").set({
      userId: ALICE_UID,
      courseCode: "COMP 315",
      courseTitle: "AI Foundations",
      semester: "Sem 1 2026/2027",
      credits: 3,
      catScore: 20,
      assignmentScore: 10,
      examScore: 40,
      totalScore: 70,
      gradeLetter: "Z",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Attendance subcollection: live check-in creation and terminal VERIFIED_IN_VENUE lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_1").set({
      userId: ALICE_UID,
      lectureId: "lec_comp314",
      courseCode: "COMP 314",
      courseTitle: "Distributed Systems",
      venueName: "Tuition Block A - TBA 204",
      recordedLat: -0.69236,
      recordedLng: 34.78471,
      accuracyMeters: 12.4,
      distanceMeters: 4.8,
      verificationStatus: "VERIFIED_IN_VENUE",
      sessionDate: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Terminal state lock prevents modifying VERIFIED_IN_VENUE attendance record
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").doc("att_1").update({
      verificationStatus: "REMOTE_LOGGED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
