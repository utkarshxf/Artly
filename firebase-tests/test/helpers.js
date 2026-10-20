// Shared setup + app-shaped writes (mirrors what the Android data layer does, per chat_spec.md "Writes").
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
  FieldPath,
  collection,
  deleteField,
  doc,
  getDoc,
  increment,
  runTransaction,
  serverTimestamp,
  setDoc,
  writeBatch,
} from 'firebase/firestore';

export const PROJECT_ID = 'demo-artistry';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');

function hostPort(envVar, fallbackPort) {
  const value = process.env[envVar];
  if (!value) return { host: '127.0.0.1', port: fallbackPort };
  const i = value.lastIndexOf(':');
  return { host: value.slice(0, i), port: Number(value.slice(i + 1)) };
}

export async function createEnv({ firestore = false, storage = false } = {}) {
  const config = { projectId: PROJECT_ID };
  if (firestore) {
    config.firestore = {
      ...hostPort('FIRESTORE_EMULATOR_HOST', 8080),
      rules: readFileSync(process.env.FIRESTORE_RULES_FILE || resolve(root, 'firestore.rules'), 'utf8'),
    };
  }
  if (storage) {
    config.storage = {
      ...hostPort('FIREBASE_STORAGE_EMULATOR_HOST', 9199),
      rules: readFileSync(process.env.STORAGE_RULES_FILE || resolve(root, 'storage.rules'), 'utf8'),
    };
  }
  return initializeTestEnvironment(config);
}

// ------------------------------------------------------------------ identities

// Chat session (custom token, uid == username). rules-unit-testing's mock token defaults to
// firebase.sign_in_provider == 'custom'.
export function chatDb(env, username) {
  return env.authenticatedContext(username).firestore();
}

// A Phone/Google Firebase session (not the chat custom-token session)
export function googleDb(env, uid) {
  return env.authenticatedContext(uid, { firebase: { sign_in_provider: 'google.com', identities: {} } }).firestore();
}

export function anonDb(env) {
  return env.unauthenticatedContext().firestore();
}

// ------------------------------------------------------------------ app-shaped data

export const cidOf = (a, b) => [a, b].sort().join('__');

export function newConversationData(a, b) {
  return {
    usernames: [a, b].sort(),
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    lastMessage: null,
    unread: {},
    lastRead: {},
    typing: {},
    muted: {},
    markedUnread: {},
    clearedAt: {},
  };
}

const PREVIEW = { text: null, image: 'Sent a photo', artwork: 'Shared a post', like: '❤️', profile: 'Shared a profile' };

export function previewOf(message) {
  if (message.type === 'text') return message.text.trim().slice(0, 120);
  return PREVIEW[message.type];
}

// Message document exactly as the app writes it
export function messageData(sender, message = {}) {
  const m = { type: 'text', text: 'hi', ...message };
  return {
    sender,
    type: m.type,
    text: m.text ?? '',
    imageUrl: m.imageUrl ?? null,
    imageWidth: m.imageWidth ?? null,
    imageHeight: m.imageHeight ?? null,
    artwork: m.artwork ?? null,
    replyTo: m.replyTo ?? null,
    reactions: {},
    createdAt: serverTimestamp(),
    unsent: false,
    // the app writes "profile" only on profile messages
    ...('profile' in m ? { profile: m.profile } : {}),
  };
}

// ChatRepository.ensureConversation: transaction get → set if missing
export async function ensureConversation(db, me, peer) {
  const cid = cidOf(me, peer);
  const ref = doc(db, 'conversations', cid);
  await runTransaction(db, async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists()) tx.set(ref, newConversationData(me, peer));
  });
  return cid;
}

// ChatRepository.send: one WriteBatch (message set + conversation update). Returns the message id.
export async function send(db, me, peer, message = {}, overrides = {}) {
  const cid = cidOf(me, peer);
  const convRef = doc(db, 'conversations', cid);
  const msgRef = doc(collection(convRef, 'messages'));
  const data = { ...messageData(me, message), ...(overrides.message ?? {}) };
  const lastMessage = {
    id: msgRef.id,
    sender: me,
    type: data.type,
    preview: previewOf({ ...data, text: data.text }),
    createdAt: serverTimestamp(),
    ...(overrides.lastMessage ?? {}),
  };
  const batch = writeBatch(db);
  batch.set(msgRef, data);
  batch.update(
    convRef,
    new FieldPath('lastMessage'), lastMessage,
    new FieldPath('updatedAt'), serverTimestamp(),
    new FieldPath('unread', peer), increment(overrides.unreadIncrement ?? 1),
    new FieldPath('unread', me), 0,
    new FieldPath('lastRead', me), serverTimestamp(),
    new FieldPath('typing', me), deleteField(),
    new FieldPath('markedUnread', me), deleteField(),
    ...(overrides.extraConversationFields ?? []),
  );
  await batch.commit();
  return msgRef.id;
}

// Seed data with rules disabled
export async function seed(env, fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

export async function readAsAdmin(env, path) {
  let data;
  await env.withSecurityRulesDisabled(async (ctx) => {
    const snap = await getDoc(doc(ctx.firestore(), path));
    data = snap.exists() ? snap.data() : undefined;
  });
  return data;
}

// Conversation alice__bob created by alice through the app flow, plus one message from alice
export async function conversationWithMessage(env, a = 'alice', b = 'bob', message = {}) {
  const db = chatDb(env, a);
  const cid = await ensureConversation(db, a, b);
  const mid = await send(db, a, b, message);
  return { cid, mid };
}

export { setDoc };
