/**
 * Community Volunteer Platform - Firebase Seed Script
 * Run this ONCE to seed the admin account and set up the Firestore structure.
 *
 * SETUP:
 *   npm install firebase-admin bcryptjs
 *   node seed_firestore.js
 */

const admin = require("firebase-admin");
const bcrypt = require("bcryptjs");

// ─────────────────────────────────────────────────────────────────────────────
// 1.  Point this at your downloaded service account key JSON
// ─────────────────────────────────────────────────────────────────────────────
const serviceAccount = require("./serviceAccountKey.json"); // <-- download from Firebase console

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();
const auth = admin.auth();

// ─────────────────────────────────────────────────────────────────────────────
// 2.  Admin credentials – change before running
// ─────────────────────────────────────────────────────────────────────────────
const ADMIN_EMAIL    = "admin@volunteerplatform.co.za";
const ADMIN_PASSWORD = "Admin@1234"; // Must meet: 8 chars, uppercase, special char, number

// ─────────────────────────────────────────────────────────────────────────────
// 3.  Firestore collection + document structure (comments show every field)
// ─────────────────────────────────────────────────────────────────────────────

/*
  COLLECTION: users
  Document ID: Firebase Auth UID
  {
    uid            : string
    role           : "volunteer" | "organization" | "admin"
    email          : string
    createdAt      : Timestamp
    updatedAt      : Timestamp

    -- volunteer only --
    firstName      : string
    lastName       : string
    phoneNumber    : string | null
    bio            : string | null
    skills         : string[]          (pre-saved list)
    profilePicUrl  : string | null
    location       : string
    deletionRequested : boolean

    -- passport (sub-fields on same doc or sub-collection) --
    totalHoursVolunteered  : number
    badgesEarned           : string[]
    opportunitiesCompleted : number

    -- organization only --
    orgName          : string
    orgNumber        : string
    logoUrl          : string | null    (uploaded or system-generated)
    orgLocation      : string
    primaryPhone     : string
    secondaryPhone   : string | null
  }

  COLLECTION: opportunities
  Document ID: auto-generated
  {
    opportunityId        : string
    orgId                : string       (uid of the organization)
    orgName              : string
    orgDescription       : string
    opportunityName      : string
    opportunityDescription: string
    category             : string       (see CATEGORIES below)
    requiresExperience   : boolean
    referencePhone       : string | null
    referenceEmail       : string | null
    requiresQualification: boolean
    qualificationUrl     : string | null  (PDF/DOCX in Firebase Storage)
    maxApplicants        : number
    approvedCount        : number
    status               : "outstanding_applicants" | "in_progress" | "completed" | "closed"
    createdAt            : Timestamp
    updatedAt            : Timestamp
  }

  CATEGORIES (accepted values):
    "Community Services"
    "Education & Mentoring"
    "Health & Social Care"
    "Environment & Conservation"
    "Emergency Response"
    "Animal Welfare"
    "Arts, Culture & Events"
    "Skills-based / Pro Bono"
    "Remote / Virtual Volunteering"

  COLLECTION: applications
  Document ID: auto-generated
  {
    applicationId   : string
    opportunityId   : string
    volunteerId     : string
    volunteerName   : string    (firstName + lastName)
    volunteerEmail  : string
    status          : "pending" | "approved" | "rejected"
    rejectionReason : string | null   (default: "Application unsuccessful")
    withdrawnStatus : boolean         (true = withdrawn)
    appliedAt       : Timestamp
    updatedAt       : Timestamp
  }

  COLLECTION: chats
  Document ID: applicationId (one chat per approved application)
  {
    applicationId  : string
    opportunityId  : string
    orgId          : string
    volunteerId    : string
    createdAt      : Timestamp
  }

  SUB-COLLECTION: chats/{applicationId}/messages
  {
    messageId  : string
    senderId   : string
    senderRole : "volunteer" | "organization"
    text       : string
    sentAt     : Timestamp
  }

  COLLECTION: notifications
  Document ID: auto-generated
  {
    userId     : string
    type       : "application_approved" | "application_rejected" | "application_received"
    message    : string
    read       : boolean
    createdAt  : Timestamp
  }
*/

// ─────────────────────────────────────────────────────────────────────────────
// 4.  Seed function
// ─────────────────────────────────────────────────────────────────────────────
async function seed() {
  console.log("🌱  Seeding Community Volunteer Platform...\n");

  try {
    // -- Create admin in Firebase Auth --
    let adminUser;
    try {
      adminUser = await auth.getUserByEmail(ADMIN_EMAIL);
      console.log("⚠️   Admin already exists in Auth – skipping Auth creation.");
    } catch {
      adminUser = await auth.createUser({
        email: ADMIN_EMAIL,
        password: ADMIN_PASSWORD,
        displayName: "Platform Admin",
        emailVerified: true,
      });
      console.log(`✅  Firebase Auth user created: ${adminUser.uid}`);
    }

    // -- Set custom claim so the app can identify admins --
    await auth.setCustomUserClaims(adminUser.uid, { role: "admin" });
    console.log("✅  Custom claim { role: 'admin' } set.");

    // -- Write admin doc to Firestore --
    const adminDocRef = db.collection("users").doc(adminUser.uid);
    const existing = await adminDocRef.get();

    if (!existing.exists) {
      await adminDocRef.set({
        uid: adminUser.uid,
        role: "admin",
        email: ADMIN_EMAIL,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      console.log("✅  Admin document written to Firestore /users collection.");
    } else {
      console.log("⚠️   Admin doc already exists in Firestore – skipping.");
    }

    // -- Placeholder collections (just ensures they appear in console) --
    // Firestore creates collections lazily, so we only create real docs above.
    // The collections below are created when the app writes its first documents.
    // Collections: opportunities, applications, chats, notifications

    console.log("\n🎉  Seeding complete!");
    console.log("─────────────────────────────────────────────");
    console.log(`   Admin Email   : ${ADMIN_EMAIL}`);
    console.log(`   Admin Password: ${ADMIN_PASSWORD}`);
    console.log(`   Admin UID     : ${adminUser.uid}`);
    console.log("─────────────────────────────────────────────");
    console.log("\n⚠️   Change the admin password after first login!");

  } catch (err) {
    console.error("❌  Seed failed:", err);
  } finally {
    process.exit(0);
  }
}

seed();
