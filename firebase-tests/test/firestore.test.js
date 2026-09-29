import assert from 'node:assert/strict';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import {
  FieldPath,
  Timestamp,
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  increment,
  limit,
  limitToLast,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';
import {
  anonDb,
  chatDb,
  cidOf,
  conversationWithMessage,
  createEnv,
  ensureConversation,
  googleDb,
  messageData,
  newConversationData,
  readAsAdmin,
  seed,
  send,
} from './helpers.js';

let env;

before(async () => {
  env = await createEnv({ firestore: true });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

const future = () => Timestamp.fromMillis(Date.now() + 24 * 3600 * 1000);

// =====================================================================================================
describe('users/{username}', () => {
  const profile = (username, extra = {}) => ({
    username,
    name: 'Alice A',
    avatar: 'https://example.com/a.jpg',
    lastActive: serverTimestamp(),
    ...extra,
  });

  it('owner upserts the profile with lastActive = serverTimestamp (set merge)', async () => {
    const db = chatDb(env, 'alice');
    await assertSucceeds(setDoc(doc(db, 'users/alice'), profile('alice'), { merge: true }));
    // second upsert (update path)
    await assertSucceeds(setDoc(doc(db, 'users/alice'), profile('alice', { name: '' , avatar: '' }), { merge: true }));
  });

  it('presence heartbeat: update lastActive only', async () => {
    const db = chatDb(env, 'alice');
    await assertSucceeds(setDoc(doc(db, 'users/alice'), profile('alice')));
    await assertSucceeds(updateDoc(doc(db, 'users/alice'), { lastActive: serverTimestamp() }));
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { lastActive: serverTimestamp() }, { merge: true }));
  });

  it('profile fields may be empty strings or null; lastActive may be omitted', async () => {
    const db = chatDb(env, 'alice');
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { username: 'alice', name: '', avatar: '' }));
    await assertSucceeds(setDoc(doc(db, 'users/alice'), { username: 'alice', name: null, avatar: null }, { merge: true }));
  });

  it('anyone signed in reads profiles; unauthenticated cannot', async () => {
    await seed(env, (db) => setDoc(doc(db, 'users/alice'), { username: 'alice', name: 'A', avatar: '' }));
    await assertSucceeds(getDoc(doc(chatDb(env, 'bob'), 'users/alice')));
    await assertSucceeds(getDoc(doc(googleDb(env, 'firebaseUid123'), 'users/alice')));
    await assertFails(getDoc(doc(anonDb(env), 'users/alice')));
  });

  it("denies writing another user's profile", async () => {
    const db = chatDb(env, 'mallory');
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice'), { merge: true }));
    await seed(env, (a) => setDoc(doc(a, 'users/alice'), { username: 'alice', name: 'A', avatar: '' }));
    await assertFails(updateDoc(doc(db, 'users/alice'), { lastActive: serverTimestamp() }));
    await assertFails(updateDoc(doc(db, 'users/alice'), { name: 'hacked' }));
  });

  it('denies a forged lastActive (client time)', async () => {
    const db = chatDb(env, 'alice');
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice', { lastActive: future() })));
    await assertSucceeds(setDoc(doc(db, 'users/alice'), profile('alice')));
    await assertFails(updateDoc(doc(db, 'users/alice'), { lastActive: future() }));
    await assertFails(updateDoc(doc(db, 'users/alice'), { lastActive: Timestamp.now() }));
  });

  it('denies username != doc id, extra fields, bad types, deletes', async () => {
    const db = chatDb(env, 'alice');
    await assertFails(setDoc(doc(db, 'users/alice'), profile('bob')));
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice', { isAdmin: true })));
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice', { name: 42 })));
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice', { avatar: 'x'.repeat(2049) })));
    await assertSucceeds(setDoc(doc(db, 'users/alice'), profile('alice')));
    await assertFails(updateDoc(doc(db, 'users/alice'), { username: 'bob' }));
    await assertFails(deleteDoc(doc(db, 'users/alice')));
  });

  it('denies profile writes from a non-chat (Google/phone) session even with a matching uid', async () => {
    const db = googleDb(env, 'alice');
    await assertFails(setDoc(doc(db, 'users/alice'), profile('alice')));
  });

  describe('private/devices', () => {
    it('owner registers and removes FCM tokens with arrayUnion / arrayRemove', async () => {
      const db = chatDb(env, 'alice');
      const ref = doc(db, 'users/alice/private/devices');
      await assertSucceeds(setDoc(ref, { tokens: arrayUnion('token-1') }, { merge: true })); // create
      await assertSucceeds(setDoc(ref, { tokens: arrayUnion('token-2') }, { merge: true }));
      await assertSucceeds(updateDoc(ref, { tokens: arrayUnion('token-1') }));
      await assertSucceeds(updateDoc(ref, { tokens: arrayRemove('token-1') }));
      await assertSucceeds(getDoc(ref));
      assert.deepEqual((await readAsAdmin(env, 'users/alice/private/devices')).tokens, ['token-2']);
    });

    it('ChatSession.registerPushToken: one batch adds the new token and drops the rotated one', async () => {
      const db = chatDb(env, 'alice');
      const ref = doc(db, 'users/alice/private/devices');
      // first registration on a fresh account: the batch creates the document
      let batch = writeBatch(db);
      batch.set(ref, { tokens: arrayUnion('old') }, { merge: true });
      await assertSucceeds(batch.commit());
      // token rotated: arrayUnion(new) + arrayRemove(previous) in the same batch
      batch = writeBatch(db);
      batch.set(ref, { tokens: arrayUnion('new') }, { merge: true });
      batch.update(ref, { tokens: arrayRemove('old') });
      await assertSucceeds(batch.commit());
      assert.deepEqual((await readAsAdmin(env, 'users/alice/private/devices')).tokens, ['new']);
    });

    it('ChatSession.signOut: set-merge arrayRemove works with or without the document; owner may delete it', async () => {
      const db = chatDb(env, 'alice');
      const ref = doc(db, 'users/alice/private/devices');
      await assertSucceeds(setDoc(ref, { tokens: arrayRemove('t') }, { merge: true })); // no document yet
      await assertSucceeds(setDoc(ref, { tokens: arrayUnion('t', 'u') }, { merge: true }));
      await assertSucceeds(setDoc(ref, { tokens: arrayRemove('t') }, { merge: true }));
      assert.deepEqual((await readAsAdmin(env, 'users/alice/private/devices')).tokens, ['u']);
      await assertFails(deleteDoc(doc(chatDb(env, 'bob'), 'users/alice/private/devices')));
      await assertFails(deleteDoc(doc(googleDb(env, 'alice'), 'users/alice/private/devices')));
      await assertSucceeds(deleteDoc(ref));
      assert.equal(await readAsAdmin(env, 'users/alice/private/devices'), undefined);
    });

    it("denies reading or writing someone else's devices", async () => {
      await seed(env, (a) => setDoc(doc(a, 'users/alice/private/devices'), { tokens: ['t'] }));
      const db = chatDb(env, 'bob');
      await assertFails(getDoc(doc(db, 'users/alice/private/devices')));
      await assertFails(updateDoc(doc(db, 'users/alice/private/devices'), { tokens: arrayUnion('evil') }));
      await assertFails(setDoc(doc(db, 'users/carol/private/devices'), { tokens: arrayUnion('evil') }));
      await assertFails(getDoc(doc(anonDb(env), 'users/alice/private/devices')));
    });

    it('denies extra fields, other private docs, non-list or oversized tokens, and non-chat sessions', async () => {
      const db = chatDb(env, 'alice');
      await assertFails(setDoc(doc(db, 'users/alice/private/devices'), { tokens: ['t'], extra: 1 }));
      await assertFails(setDoc(doc(db, 'users/alice/private/settings'), { tokens: ['t'] }));
      await assertFails(setDoc(doc(db, 'users/alice/private/devices'), { tokens: 't' }));
      await assertFails(setDoc(doc(db, 'users/alice/private/devices'),
        { tokens: Array.from({ length: 101 }, (_, i) => `t${i}`) }));
      await assertFails(setDoc(doc(googleDb(env, 'alice'), 'users/alice/private/devices'), { tokens: ['t'] }));
      await assertSucceeds(setDoc(doc(db, 'users/alice/private/devices'), { tokens: ['t'] }));
    });
  });
});

// =====================================================================================================
describe('conversations/{cid}: create', () => {
  it('ensureConversation (transaction get → set) creates alice__bob', async () => {
    const db = chatDb(env, 'bob');
    const cid = await assertSucceeds(ensureConversation(db, 'bob', 'alice'));
    assert.equal(cid, 'alice__bob');
    const data = await readAsAdmin(env, 'conversations/alice__bob');
    assert.deepEqual(data.usernames, ['alice', 'bob']);
    // second call is a no-op
    await assertSucceeds(ensureConversation(chatDb(env, 'alice'), 'alice', 'bob'));
  });

  it('plain set without updatedAt / lastMessage is also a valid create', async () => {
    const db = chatDb(env, 'alice');
    const { updatedAt, lastMessage, ...rest } = newConversationData('alice', 'bob');
    await assertSucceeds(setDoc(doc(db, 'conversations/alice__bob'), rest));
  });

  it('a party can read a conversation that does not exist yet; others cannot', async () => {
    await assertSucceeds(getDoc(doc(chatDb(env, 'alice'), 'conversations/alice__bob')));
    await assertSucceeds(getDoc(doc(chatDb(env, 'bob'), 'conversations/alice__bob')));
    await assertFails(getDoc(doc(chatDb(env, 'carol'), 'conversations/alice__bob')));
    await assertFails(getDoc(doc(chatDb(env, 'ali'), 'conversations/alice__bob')));
    await assertFails(getDoc(doc(anonDb(env), 'conversations/alice__bob')));
  });

  it('denies a non-member creating a conversation', async () => {
    const db = chatDb(env, 'carol');
    await assertFails(setDoc(doc(db, 'conversations/alice__bob'), newConversationData('alice', 'bob')));
  });

  it('denies unsorted usernames, a mismatched id and self-chat', async () => {
    const db = chatDb(env, 'alice');
    await assertFails(setDoc(doc(db, 'conversations/bob__alice'), { ...newConversationData('alice', 'bob'), usernames: ['bob', 'alice'] }));
    await assertFails(setDoc(doc(db, 'conversations/alice__bob'), { ...newConversationData('alice', 'bob'), usernames: ['bob', 'alice'] }));
    await assertFails(setDoc(doc(db, 'conversations/alice_bob'), newConversationData('alice', 'bob')));
    await assertFails(setDoc(doc(db, 'conversations/alice__bobby'), newConversationData('alice', 'bob')));
    await assertFails(setDoc(doc(db, 'conversations/alice__alice'), { ...newConversationData('alice', 'alice'), usernames: ['alice', 'alice'] }));
    await assertFails(setDoc(doc(db, 'conversations/alice__bob__carol'), { ...newConversationData('alice', 'bob'), usernames: ['alice', 'bob', 'carol'] }));
    // a third member smuggled in behind a valid id
    await assertFails(setDoc(doc(db, 'conversations/alice__bob'), { ...newConversationData('alice', 'bob'), usernames: ['alice', 'bob', 'carol'] }));
    await assertFails(setDoc(doc(db, 'conversations/alice'), { ...newConversationData('alice', 'bob'), usernames: ['alice'] }));
  });

  it('denies non-empty maps, missing maps, a lastMessage, extra fields and client timestamps', async () => {
    const db = chatDb(env, 'alice');
    const ref = doc(db, 'conversations/alice__bob');
    const base = newConversationData('alice', 'bob');
    await assertFails(setDoc(ref, { ...base, unread: { bob: 5 } }));
    await assertFails(setDoc(ref, { ...base, muted: { bob: true } }));
    await assertFails(setDoc(ref, { ...base, lastRead: { bob: serverTimestamp() } }));
    const { typing, ...missingTyping } = base;
    await assertFails(setDoc(ref, missingTyping));
    await assertFails(setDoc(ref, { ...base, lastMessage: { id: 'x', sender: 'alice', type: 'text', preview: 'hi', createdAt: serverTimestamp() } }));
    await assertFails(setDoc(ref, { ...base, admin: 'alice' }));
    await assertFails(setDoc(ref, { ...base, createdAt: Timestamp.now() }));
    await assertFails(setDoc(ref, { ...base, updatedAt: future() }));
  });

  it('denies a non-chat (Google/phone) session', async () => {
    const db = googleDb(env, 'alice');
    await assertFails(setDoc(doc(db, 'conversations/alice__bob'), newConversationData('alice', 'bob')));
  });

  it('a blind set over an existing conversation (reset) is denied — use a transaction', async () => {
    await ensureConversation(chatDb(env, 'alice'), 'alice', 'bob');
    await assertFails(setDoc(doc(chatDb(env, 'bob'), 'conversations/alice__bob'), newConversationData('alice', 'bob')));
  });
});

// =====================================================================================================
describe('conversations: read + inbox query', () => {
  it('members read the conversation; non-members and anonymous cannot', async () => {
    const { cid } = await conversationWithMessage(env);
    await assertSucceeds(getDoc(doc(chatDb(env, 'alice'), 'conversations', cid)));
    await assertSucceeds(getDoc(doc(chatDb(env, 'bob'), 'conversations', cid)));
    await assertFails(getDoc(doc(chatDb(env, 'carol'), 'conversations', cid)));
    await assertFails(getDoc(doc(anonDb(env), 'conversations', cid)));
    await assertFails(getDoc(doc(googleDb(env, 'alice'), 'conversations', cid)));
  });

  it('inbox query (array-contains me, orderBy updatedAt desc, limit 100) is allowed', async () => {
    await conversationWithMessage(env, 'alice', 'bob');
    await conversationWithMessage(env, 'carol', 'alice');
    await conversationWithMessage(env, 'bob', 'carol');
    const db = chatDb(env, 'alice');
    const q = query(collection(db, 'conversations'), where('usernames', 'array-contains', 'alice'),
      orderBy('updatedAt', 'desc'), limit(100));
    const snap = await assertSucceeds(getDocs(q));
    assert.deepEqual(snap.docs.map((d) => d.id).sort(), ['alice__bob', 'alice__carol']);
  });

  it('inbox query of a user without conversations is allowed (empty)', async () => {
    const snap = await assertSucceeds(getDocs(query(collection(chatDb(env, 'dave'), 'conversations'),
      where('usernames', 'array-contains', 'dave'), orderBy('updatedAt', 'desc'), limit(100))));
    assert.equal(snap.size, 0);
  });

  it('denies listing without the membership filter or for another user', async () => {
    await conversationWithMessage(env);
    const db = chatDb(env, 'alice');
    await assertFails(getDocs(collection(db, 'conversations')));
    await assertFails(getDocs(query(collection(db, 'conversations'), orderBy('updatedAt', 'desc'))));
    await assertFails(getDocs(query(collection(db, 'conversations'), where('usernames', 'array-contains', 'bob'))));
    await assertFails(getDocs(query(collection(db, 'conversations'), where('usernames', 'array-contains-any', ['alice', 'bob']))));
    await assertFails(getDocs(query(collection(anonDb(env), 'conversations'), where('usernames', 'array-contains', 'alice'))));
  });
});

// =====================================================================================================
describe('send (WriteBatch: message set + conversation update)', () => {
  it('first message right after the conversation create, then replies; counters and preview', async () => {
    const alice = chatDb(env, 'alice');
    const bob = chatDb(env, 'bob');
    const cid = await assertSucceeds(ensureConversation(alice, 'alice', 'bob'));
    const m1 = await assertSucceeds(send(alice, 'alice', 'bob', { type: 'text', text: '  hey there  ' }));
    let c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.unread.bob, 1);
    assert.equal(c.unread.alice, 0);
    assert.equal(c.lastMessage.id, m1);
    assert.equal(c.lastMessage.preview, 'hey there');
    assert.ok(c.lastRead.alice);

    await assertSucceeds(send(alice, 'alice', 'bob', { type: 'like' }));
    c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.unread.bob, 2);

    await assertSucceeds(send(bob, 'bob', 'alice', { type: 'text', text: 'yo' }));
    c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.unread.alice, 1);
    assert.equal(c.unread.bob, 0);
    assert.equal(c.lastMessage.sender, 'bob');
  });

  it('all message types: image, artwork (shared post), like, reply', async () => {
    const alice = chatDb(env, 'alice');
    await ensureConversation(alice, 'alice', 'bob');
    const m1 = await assertSucceeds(send(alice, 'alice', 'bob', {
      type: 'image', text: '',
      imageUrl: 'https://firebasestorage.googleapis.com/v0/b/x/o/chat%2Falice__bob%2Falice%2Fa.jpg?alt=media&token=t',
      imageWidth: 1200, imageHeight: 1600,
    }));
    await assertSucceeds(send(alice, 'alice', 'bob', {
      type: 'artwork', text: '',
      artwork: { id: 'art-1', title: 'Starry Night', imageUrl: 'https://example.com/s.jpg', artistName: 'Vincent' },
    }));
    await assertSucceeds(send(alice, 'alice', 'bob', {
      type: 'artwork', text: '', artwork: { id: 'art-2', title: null, imageUrl: null, artistName: null },
    }));
    await assertSucceeds(send(chatDb(env, 'bob'), 'bob', 'alice', {
      type: 'text', text: 'nice',
      replyTo: { id: m1, sender: 'alice', type: 'image', preview: 'Sent a photo' },
    }));
    await assertSucceeds(send(alice, 'alice', 'bob', { type: 'like', text: '' }));
    // image caption
    await assertSucceeds(send(alice, 'alice', 'bob', { type: 'image', text: 'look', imageUrl: 'https://e.com/p.jpg' }));
    // replyTo may carry the quoted message's createdAt
    await assertSucceeds(send(alice, 'alice', 'bob', {
      text: 'ok', replyTo: { id: m1, sender: 'alice', type: 'image', preview: 'Sent a photo', createdAt: Timestamp.now() },
    }));
    // null optional fields may be omitted entirely
    const minimal = doc(collection(alice, 'conversations/alice__bob/messages'));
    await assertSucceeds(setDoc(minimal, { sender: 'alice', type: 'like', reactions: {}, createdAt: serverTimestamp(), unsent: false }));
  });

  it('denies an oversized lastMessage preview', async () => {
    const alice = chatDb(env, 'alice');
    await ensureConversation(alice, 'alice', 'bob');
    await assertFails(send(alice, 'alice', 'bob', { text: 'hello' }, { lastMessage: { preview: 'p'.repeat(201) } }));
    await assertSucceeds(send(alice, 'alice', 'bob', { text: 'hello' }, { lastMessage: { preview: 'p'.repeat(200) } }));
  });

  it('text of exactly 4000 characters (incl. multi-byte) is allowed; 4001 is denied', async () => {
    const alice = chatDb(env, 'alice');
    await ensureConversation(alice, 'alice', 'bob');
    await assertSucceeds(send(alice, 'alice', 'bob', { text: 'a'.repeat(4000) }));
    await assertSucceeds(send(alice, 'alice', 'bob', { text: 'अ'.repeat(4000) }));
    await assertFails(send(alice, 'alice', 'bob', { text: 'a'.repeat(4001) }));
  });

  it('create conversation + first message in the same batch also works (getAfter)', async () => {
    const alice = chatDb(env, 'alice');
    const convRef = doc(alice, 'conversations/alice__bob');
    const msgRef = doc(collection(convRef, 'messages'));
    const batch = writeBatch(alice);
    batch.set(convRef, newConversationData('alice', 'bob'));
    batch.set(msgRef, messageData('alice'));
    await assertSucceeds(batch.commit());
  });

  it('usernames containing dots work with FieldPath map keys', async () => {
    const a = chatDb(env, 'john.doe');
    const cid = await assertSucceeds(ensureConversation(a, 'john.doe', 'mary_k'));
    assert.equal(cid, 'john.doe__mary_k');
    await assertSucceeds(send(a, 'john.doe', 'mary_k', { text: 'hi' }));
    await assertSucceeds(send(chatDb(env, 'mary_k'), 'mary_k', 'john.doe', { text: 'hey' }));
    const c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.unread['john.doe'], 1);
  });

  it('denies sender spoofing (message.sender or lastMessage.sender)', async () => {
    const alice = chatDb(env, 'alice');
    await ensureConversation(alice, 'alice', 'bob');
    await assertFails(send(alice, 'alice', 'bob', {}, { message: { sender: 'bob' }, lastMessage: { sender: 'bob' } }));
    await assertFails(send(alice, 'alice', 'bob', {}, { lastMessage: { sender: 'bob' } }));
    // a lone message create claiming to be bob
    await assertFails(setDoc(doc(alice, 'conversations/alice__bob/messages/m1'), messageData('bob')));
  });

  it('denies a non-member posting into a conversation', async () => {
    await ensureConversation(chatDb(env, 'alice'), 'alice', 'bob');
    const carol = chatDb(env, 'carol');
    await assertFails(setDoc(doc(carol, 'conversations/alice__bob/messages/m1'), messageData('carol')));
    // full send batch into alice__bob
    const convRef = doc(carol, 'conversations/alice__bob');
    const msgRef = doc(collection(convRef, 'messages'));
    const batch = writeBatch(carol);
    batch.set(msgRef, messageData('carol'));
    batch.update(convRef,
      new FieldPath('lastMessage'), { id: msgRef.id, sender: 'carol', type: 'text', preview: 'hi', createdAt: serverTimestamp() },
      new FieldPath('updatedAt'), serverTimestamp(),
      new FieldPath('unread', 'bob'), increment(1));
    await assertFails(batch.commit());
  });

  it('denies a message in a conversation that does not exist', async () => {
    const alice = chatDb(env, 'alice');
    await assertFails(setDoc(doc(alice, 'conversations/alice__bob/messages/m1'), messageData('alice')));
  });

  it('denies client createdAt, unsent/reactions on create, bad type, extra fields, bad content', async () => {
    const alice = chatDb(env, 'alice');
    await ensureConversation(alice, 'alice', 'bob');
    const ref = doc(alice, 'conversations/alice__bob/messages/m1');
    await assertFails(setDoc(ref, { ...messageData('alice'), createdAt: Timestamp.now() }));
    await assertFails(setDoc(ref, { ...messageData('alice'), createdAt: future() }));
    await assertFails(setDoc(ref, { ...messageData('alice'), unsent: true }));
    await assertFails(setDoc(ref, { ...messageData('alice'), reactions: { bob: '❤️' } }));
    await assertFails(setDoc(ref, { ...messageData('alice'), type: 'video' }));
    await assertFails(setDoc(ref, { ...messageData('alice'), type: 'unsent' }));
    await assertFails(setDoc(ref, { ...messageData('alice'), edited: true }));
    await assertFails(setDoc(ref, { ...messageData('alice'), text: '' })); // empty text message
    await assertFails(setDoc(ref, messageData('alice', { type: 'image', text: '', imageUrl: null })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'artwork', text: '', artwork: null })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'artwork', artwork: { id: 'a', price: 5 } })));
    await assertFails(setDoc(ref, messageData('alice', { replyTo: { id: 'x', sender: 'bob', type: 'text', preview: 'p', extra: 1 } })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'image', imageUrl: 'u', imageWidth: 'wide' })));
    const { unsent, ...noUnsent } = messageData('alice');
    await assertFails(setDoc(ref, noUnsent));
    // the valid baseline passes
    await assertSucceeds(setDoc(ref, messageData('alice')));
  });

  it('denies a lastMessage that points to no message / a peer message / mismatching type', async () => {
    const { cid } = await conversationWithMessage(env);
    const bobMsg = await send(chatDb(env, 'bob'), 'bob', 'alice', { text: 'hello' });
    const alice = chatDb(env, 'alice');
    const convRef = doc(alice, 'conversations', cid);
    const lm = (id, type = 'text') => ({ id, sender: 'alice', type, preview: 'fake', createdAt: serverTimestamp() });
    await assertFails(updateDoc(convRef, { lastMessage: lm('does-not-exist'), updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(convRef, { lastMessage: lm(bobMsg), updatedAt: serverTimestamp() }));
    await assertFails(send(alice, 'alice', 'bob', { type: 'text', text: 'x' }, { lastMessage: { type: 'like' } }));
    await assertFails(send(alice, 'alice', 'bob', { type: 'text', text: 'x' }, { lastMessage: { createdAt: Timestamp.now() } }));
    await assertFails(send(alice, 'alice', 'bob', {}, { lastMessage: { admin: true } }));
    await assertFails(updateDoc(convRef, { lastMessage: null }));
  });

  it('denies tampering with unread counters', async () => {
    const { cid } = await conversationWithMessage(env); // unread.bob == 1
    const alice = chatDb(env, 'alice');
    const convRef = doc(alice, 'conversations', cid);
    await assertFails(updateDoc(convRef, new FieldPath('unread', 'bob'), 99));
    await assertFails(updateDoc(convRef, new FieldPath('unread', 'bob'), 0));
    await assertFails(updateDoc(convRef, new FieldPath('unread', 'bob'), increment(1))); // without a send
    await assertFails(updateDoc(convRef, new FieldPath('unread', 'alice'), 3));
    await assertFails(updateDoc(convRef, new FieldPath('unread', 'carol'), 1));
    // a send that adds more than one unread
    await assertFails(send(alice, 'alice', 'bob', {}, { unreadIncrement: 2 }));
    await assertFails(send(alice, 'alice', 'bob', {}, { unreadIncrement: -1 }));
    // a send that also clears the peer's lastRead or touches a third user's key
    await assertFails(send(alice, 'alice', 'bob', {}, { extraConversationFields: [new FieldPath('lastRead', 'bob'), serverTimestamp()] }));
    await assertFails(send(alice, 'alice', 'bob', {}, { extraConversationFields: [new FieldPath('unread', 'carol'), 1] }));
    await assertFails(send(alice, 'alice', 'bob', {}, { extraConversationFields: [new FieldPath('muted', 'bob'), true] }));
    // the same send without tampering passes
    await assertSucceeds(send(alice, 'alice', 'bob', {}));
    assert.equal((await readAsAdmin(env, `conversations/${cid}`)).unread.bob, 2);
  });

  it('denies changing usernames or createdAt, bumping updatedAt without a send, and deleting', async () => {
    const { cid } = await conversationWithMessage(env);
    const alice = chatDb(env, 'alice');
    const convRef = doc(alice, 'conversations', cid);
    await assertFails(updateDoc(convRef, { usernames: ['alice', 'mallory'] }));
    await assertFails(updateDoc(convRef, { usernames: arrayUnion('carol') }));
    await assertFails(updateDoc(convRef, { createdAt: serverTimestamp() }));
    await assertFails(updateDoc(convRef, { updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(convRef, { pinned: true }));
    await assertFails(deleteDoc(convRef));
    await assertFails(deleteDoc(doc(chatDb(env, 'bob'), 'conversations', cid)));
  });

  it('denies non-members updating the conversation', async () => {
    const { cid } = await conversationWithMessage(env);
    const carol = chatDb(env, 'carol');
    const convRef = doc(carol, 'conversations', cid);
    await assertFails(updateDoc(convRef, new FieldPath('muted', 'carol'), true));
    await assertFails(updateDoc(convRef, new FieldPath('typing', 'carol'), serverTimestamp()));
  });
});

// =====================================================================================================
describe('per-user conversation state', () => {
  it('mark read: unread.me = 0, lastRead.me = serverTimestamp, markedUnread.me deleted', async () => {
    const { cid } = await conversationWithMessage(env);
    const bob = chatDb(env, 'bob');
    const ref = doc(bob, 'conversations', cid);
    await assertSucceeds(updateDoc(ref,
      new FieldPath('markedUnread', 'bob'), true));
    await assertSucceeds(updateDoc(ref,
      new FieldPath('unread', 'bob'), 0,
      new FieldPath('lastRead', 'bob'), serverTimestamp(),
      new FieldPath('markedUnread', 'bob'), deleteField()));
    const c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.unread.bob, 0);
    assert.ok(c.lastRead.bob);
    assert.equal(c.markedUnread.bob, undefined);
  });

  it("denies changing the other user's lastRead / forged read time", async () => {
    const { cid } = await conversationWithMessage(env);
    const alice = chatDb(env, 'alice');
    const ref = doc(alice, 'conversations', cid);
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'bob'), serverTimestamp()));
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'alice'), future()));
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'alice'), deleteField()));
    await assertFails(updateDoc(ref, new FieldPath('unread', 'bob'), 0)); // clearing bob's badge
    // once bob has read: deleting his receipt or replacing the whole map (dropping it) is denied
    await updateDoc(doc(chatDb(env, 'bob'), 'conversations', cid), new FieldPath('lastRead', 'bob'), serverTimestamp());
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'bob'), deleteField()));
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'bob'), Timestamp.fromMillis(0)));
    await assertFails(updateDoc(ref, { lastRead: { alice: serverTimestamp() } }));
    await assertSucceeds(updateDoc(ref, new FieldPath('lastRead', 'alice'), serverTimestamp()));
  });

  it('typing: set with serverTimestamp and delete; nothing else', async () => {
    const { cid } = await conversationWithMessage(env);
    const bob = chatDb(env, 'bob');
    const ref = doc(bob, 'conversations', cid);
    await assertSucceeds(updateDoc(ref, new FieldPath('typing', 'bob'), serverTimestamp()));
    await assertSucceeds(updateDoc(ref, new FieldPath('typing', 'bob'), deleteField()));
    await assertFails(updateDoc(ref, new FieldPath('typing', 'bob'), future()));
    await assertFails(updateDoc(ref, new FieldPath('typing', 'alice'), serverTimestamp()));
    await assertSucceeds(updateDoc(doc(chatDb(env, 'alice'), 'conversations', cid), new FieldPath('typing', 'alice'), serverTimestamp()));
    await assertFails(updateDoc(ref, new FieldPath('typing', 'alice'), deleteField()));
    await assertFails(updateDoc(ref, { typing: {} }));
  });

  it('mute / mark unread / delete chat (clearedAt) for me', async () => {
    const { cid } = await conversationWithMessage(env);
    const bob = chatDb(env, 'bob');
    const ref = doc(bob, 'conversations', cid);
    await assertSucceeds(updateDoc(ref, new FieldPath('muted', 'bob'), true));
    await assertSucceeds(updateDoc(ref, new FieldPath('muted', 'bob'), false));
    await assertSucceeds(updateDoc(ref, new FieldPath('markedUnread', 'bob'), true));
    await assertSucceeds(updateDoc(ref, new FieldPath('markedUnread', 'bob'), false));
    await assertSucceeds(updateDoc(ref, new FieldPath('clearedAt', 'bob'), serverTimestamp()));
    await assertSucceeds(updateDoc(ref,
      new FieldPath('clearedAt', 'bob'), serverTimestamp(),
      new FieldPath('unread', 'bob'), 0,
      new FieldPath('markedUnread', 'bob'), deleteField()));
  });

  it("denies changing the other user's muted / markedUnread / clearedAt, or bad values", async () => {
    const { cid } = await conversationWithMessage(env);
    const bob = chatDb(env, 'bob');
    const ref = doc(bob, 'conversations', cid);
    await assertFails(updateDoc(ref, new FieldPath('muted', 'alice'), true));
    await assertFails(updateDoc(ref, new FieldPath('markedUnread', 'alice'), true));
    await assertFails(updateDoc(ref, new FieldPath('clearedAt', 'alice'), serverTimestamp()));
    await assertFails(updateDoc(ref, new FieldPath('muted', 'bob'), 'yes'));
    await assertFails(updateDoc(ref, new FieldPath('clearedAt', 'bob'), 'now'));
    await assertFails(updateDoc(ref, { muted: { alice: false, bob: true } }));
    // the caller's own key plus the other user's key in one write
    await assertFails(updateDoc(ref, new FieldPath('clearedAt', 'bob'), serverTimestamp(), new FieldPath('clearedAt', 'alice'), serverTimestamp()));
    await assertFails(updateDoc(ref, new FieldPath('muted', 'bob'), true, new FieldPath('muted', 'alice'), true));
    await assertFails(updateDoc(ref, new FieldPath('markedUnread', 'bob'), true, new FieldPath('markedUnread', 'alice'), true));
    await assertFails(updateDoc(ref, new FieldPath('lastRead', 'bob'), serverTimestamp(), new FieldPath('lastRead', 'alice'), serverTimestamp()));
    await assertFails(updateDoc(ref, new FieldPath('typing', 'bob'), serverTimestamp(), new FieldPath('typing', 'alice'), serverTimestamp()));
    // each of them alone is fine
    await assertSucceeds(updateDoc(ref, new FieldPath('clearedAt', 'bob'), serverTimestamp()));
  });
});

// =====================================================================================================
describe('messages: read', () => {
  it('members read the thread query (orderBy createdAt, createdAt > clearedAt, limitToLast)', async () => {
    const { cid } = await conversationWithMessage(env);
    await send(chatDb(env, 'bob'), 'bob', 'alice', { text: 'reply' });
    for (const who of ['alice', 'bob']) {
      const db = chatDb(env, who);
      const q = query(collection(db, 'conversations', cid, 'messages'),
        orderBy('createdAt'), where('createdAt', '>', Timestamp.fromMillis(0)), limitToLast(40));
      const snap = await assertSucceeds(getDocs(q));
      assert.equal(snap.size, 2);
      await assertSucceeds(getDocs(query(collection(db, 'conversations', cid, 'messages'), orderBy('createdAt'), limitToLast(40))));
    }
  });

  it('non-members and anonymous cannot read messages', async () => {
    const { cid, mid } = await conversationWithMessage(env);
    const carol = chatDb(env, 'carol');
    await assertFails(getDocs(query(collection(carol, 'conversations', cid, 'messages'), orderBy('createdAt'), limitToLast(40))));
    await assertFails(getDoc(doc(carol, 'conversations', cid, 'messages', mid)));
    await assertFails(getDoc(doc(anonDb(env), 'conversations', cid, 'messages', mid)));
    await assertFails(getDoc(doc(googleDb(env, 'alice'), 'conversations', cid, 'messages', mid)));
  });

  it('a party can open the (empty) thread of a conversation that does not exist yet', async () => {
    const snap = await assertSucceeds(getDocs(query(collection(chatDb(env, 'alice'), 'conversations/alice__bob/messages'),
      orderBy('createdAt'), limitToLast(40))));
    assert.equal(snap.size, 0);
    await assertFails(getDocs(query(collection(chatDb(env, 'carol'), 'conversations/alice__bob/messages'),
      orderBy('createdAt'), limitToLast(40))));
  });
});

// =====================================================================================================
describe('messages: reactions', () => {
  it('both members add, change and remove their own reaction', async () => {
    const { cid, mid } = await conversationWithMessage(env);
    for (const who of ['alice', 'bob']) {
      const ref = doc(chatDb(env, who), 'conversations', cid, 'messages', mid);
      await assertSucceeds(updateDoc(ref, new FieldPath('reactions', who), '❤️'));
      await assertSucceeds(updateDoc(ref, new FieldPath('reactions', who), '👍'));
    }
    let m = await readAsAdmin(env, `conversations/${cid}/messages/${mid}`);
    assert.deepEqual(m.reactions, { alice: '👍', bob: '👍' });
    await assertSucceeds(updateDoc(doc(chatDb(env, 'bob'), 'conversations', cid, 'messages', mid),
      new FieldPath('reactions', 'bob'), deleteField()));
    m = await readAsAdmin(env, `conversations/${cid}/messages/${mid}`);
    assert.deepEqual(m.reactions, { alice: '👍' });
  });

  it('denies reacting as someone else, by a non-member, or with edits', async () => {
    const { cid, mid } = await conversationWithMessage(env);
    await updateDoc(doc(chatDb(env, 'alice'), 'conversations', cid, 'messages', mid), new FieldPath('reactions', 'alice'), '😂');
    const bobRef = doc(chatDb(env, 'bob'), 'conversations', cid, 'messages', mid);
    await assertFails(updateDoc(bobRef, new FieldPath('reactions', 'alice'), '😡'));
    await assertFails(updateDoc(bobRef, new FieldPath('reactions', 'alice'), deleteField()));
    await assertFails(updateDoc(bobRef, { reactions: { bob: '❤️' } })); // wipes alice's
    await assertFails(updateDoc(bobRef, new FieldPath('reactions', 'bob'), '❤️', 'text', 'edited'));
    await assertFails(updateDoc(bobRef, new FieldPath('reactions', 'bob'), 5));
    await assertFails(updateDoc(bobRef, new FieldPath('reactions', 'bob'), ''));
    const carolRef = doc(chatDb(env, 'carol'), 'conversations', cid, 'messages', mid);
    await assertFails(updateDoc(carolRef, new FieldPath('reactions', 'carol'), '❤️'));
  });
});

// =====================================================================================================
describe('messages: unsend + edits + deletes', () => {
  function unsendBatch(db, cid, mid, { updatePreview }) {
    const batch = writeBatch(db);
    batch.update(doc(db, 'conversations', cid, 'messages', mid), {
      unsent: true, text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {},
    });
    if (updatePreview) {
      batch.update(doc(db, 'conversations', cid), new FieldPath('lastMessage', 'preview'), 'Unsent a message');
    }
    return batch.commit();
  }

  it('sender unsends the latest message and updates the inbox preview', async () => {
    const { cid, mid } = await conversationWithMessage(env, 'alice', 'bob', { type: 'text', text: 'oops' });
    await updateDoc(doc(chatDb(env, 'bob'), 'conversations', cid, 'messages', mid), new FieldPath('reactions', 'bob'), '😮');
    await assertSucceeds(unsendBatch(chatDb(env, 'alice'), cid, mid, { updatePreview: true }));
    const m = await readAsAdmin(env, `conversations/${cid}/messages/${mid}`);
    assert.equal(m.unsent, true);
    assert.equal(m.text, '');
    assert.deepEqual(m.reactions, {});
    const c = await readAsAdmin(env, `conversations/${cid}`);
    assert.equal(c.lastMessage.preview, 'Unsent a message');
    assert.equal(c.lastMessage.type, 'text');
  });

  it('sender unsends an older image / artwork message (two separate writes work too)', async () => {
    const alice = chatDb(env, 'alice');
    const cid = await ensureConversation(alice, 'alice', 'bob');
    const img = await send(alice, 'alice', 'bob', { type: 'image', text: '', imageUrl: 'https://e.com/i.jpg', imageWidth: 10, imageHeight: 20 });
    const art = await send(alice, 'alice', 'bob', { type: 'artwork', text: '', artwork: { id: 'a1', title: 't', imageUrl: 'u', artistName: 'n' } });
    await send(chatDb(env, 'bob'), 'bob', 'alice', { text: 'latest', replyTo: { id: img, sender: 'alice', type: 'image', preview: 'Sent a photo' } });
    await assertSucceeds(unsendBatch(alice, cid, img, { updatePreview: false }));
    await assertSucceeds(updateDoc(doc(alice, 'conversations', cid, 'messages', art), {
      unsent: true, text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {},
    }));
    // alternative encoding: deleted fields + width/height cleared
    const img2 = await send(alice, 'alice', 'bob', { type: 'image', text: '', imageUrl: 'https://e.com/j.jpg', imageWidth: 10, imageHeight: 20 });
    await assertSucceeds(updateDoc(doc(alice, 'conversations', cid, 'messages', img2), {
      unsent: true, text: deleteField(), imageUrl: deleteField(), imageWidth: null, imageHeight: null,
      artwork: deleteField(), replyTo: deleteField(), reactions: {},
    }));
    // unsending again is a harmless no-op
    await assertSucceeds(unsendBatch(alice, cid, img, { updatePreview: false }));
  });

  it('shared profile: send and unsend allowed, malformed profiles denied', async () => {
    const alice = chatDb(env, 'alice');
    const cid = await ensureConversation(alice, 'alice', 'bob');
    const full = await assertSucceeds(send(alice, 'alice', 'bob', {
      type: 'profile', text: '',
      profile: { id: 'rembrandt-1', name: 'Rembrandt', avatar: 'https://e.com/r.jpg', subtitle: 'Artist' },
    }));
    // only the id is required
    await assertSucceeds(send(alice, 'alice', 'bob', { type: 'profile', text: '', profile: { id: 'carol' } }));
    // unsend exactly as the app writes it for a profile message
    await assertSucceeds(updateDoc(doc(alice, 'conversations', cid, 'messages', full), {
      unsent: true, text: '', imageUrl: null, artwork: null, profile: null, replyTo: null, reactions: {},
    }));

    const ref = doc(alice, 'conversations', cid, 'messages', 'bad-profile');
    await assertFails(setDoc(ref, messageData('alice', { type: 'profile', text: '', profile: null })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'profile', text: '', profile: { id: '' } })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'profile', text: '', profile: { id: 'x', followers: 5 } })));
    await assertFails(setDoc(ref, messageData('alice', { type: 'profile', text: '', profile: { id: 'x', name: 7 } })));

    // unsend that leaves the profile card behind is denied
    const kept = await send(alice, 'alice', 'bob', { type: 'profile', text: '', profile: { id: 'dave' } });
    await assertFails(updateDoc(doc(alice, 'conversations', cid, 'messages', kept), {
      unsent: true, text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {},
    }));
  });

  it("denies unsending someone else's message or rewriting another's preview", async () => {
    const { cid, mid } = await conversationWithMessage(env, 'alice', 'bob', { text: 'mine' });
    const bob = chatDb(env, 'bob');
    await assertFails(unsendBatch(bob, cid, mid, { updatePreview: false }));
    await assertFails(updateDoc(doc(bob, 'conversations', cid), new FieldPath('lastMessage', 'preview'), 'Unsent a message'));
    const carol = chatDb(env, 'carol');
    await assertFails(unsendBatch(carol, cid, mid, { updatePreview: false }));
  });

  it('denies editing text, partial unsend, un-unsending and other preview rewrites', async () => {
    const { cid, mid } = await conversationWithMessage(env, 'alice', 'bob', { text: 'original' });
    const alice = chatDb(env, 'alice');
    const ref = doc(alice, 'conversations', cid, 'messages', mid);
    await assertFails(updateDoc(ref, { text: 'edited' }));
    await assertFails(updateDoc(ref, { sender: 'bob' }));
    await assertFails(updateDoc(ref, { createdAt: serverTimestamp() }));
    await assertFails(updateDoc(ref, { type: 'like' }));
    await assertFails(updateDoc(ref, { unsent: true })); // text left behind
    await assertFails(updateDoc(ref, { unsent: true, text: '', imageUrl: 'https://evil' }));
    await assertFails(updateDoc(ref, { unsent: true, text: '', reactions: { alice: '❤️' } }));
    // wiping the content without flagging the message as unsent (a silent edit)
    await assertFails(updateDoc(ref, { text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {} }));
    await assertFails(updateDoc(ref, { unsent: 'yes', text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {} }));
    // an unsend may not touch the message's identity fields
    const wipe = { unsent: true, text: '', imageUrl: null, artwork: null, replyTo: null, reactions: {} };
    await assertFails(updateDoc(ref, { ...wipe, type: 'like' }));
    await assertFails(updateDoc(ref, { ...wipe, sender: 'bob' }));
    await assertFails(updateDoc(ref, { ...wipe, createdAt: serverTimestamp() }));
    await assertFails(updateDoc(ref, { ...wipe, extra: 1 }));
    const convRef = doc(alice, 'conversations', cid);
    await assertFails(updateDoc(convRef, new FieldPath('lastMessage', 'preview'), 'something else'));
    await assertFails(updateDoc(convRef, new FieldPath('lastMessage', 'type'), 'unsent'));
    // the unsend preview may not carry other lastMessage changes along
    await assertFails(updateDoc(convRef, new FieldPath('lastMessage', 'preview'), 'Unsent a message', new FieldPath('lastMessage', 'type'), 'unsent'));
    await assertFails(updateDoc(convRef, new FieldPath('lastMessage', 'preview'), 'Unsent a message', new FieldPath('lastMessage', 'id'), 'other'));
    await assertFails(updateDoc(convRef, new FieldPath('lastMessage', 'preview'), 'Unsent a message', 'updatedAt', serverTimestamp()));
    await assertSucceeds(unsendBatch(alice, cid, mid, { updatePreview: true }));
    await assertFails(updateDoc(ref, { unsent: false, text: 'back' }));
    await assertFails(updateDoc(doc(chatDb(env, 'bob'), 'conversations', cid, 'messages', mid), new FieldPath('reactions', 'bob'), '❤️'));
  });

  it('denies deleting messages', async () => {
    const { cid, mid } = await conversationWithMessage(env);
    await assertFails(deleteDoc(doc(chatDb(env, 'alice'), 'conversations', cid, 'messages', mid)));
    await assertFails(deleteDoc(doc(chatDb(env, 'bob'), 'conversations', cid, 'messages', mid)));
  });
});

// =====================================================================================================
describe('everything else', () => {
  it('unknown collections are denied', async () => {
    const db = chatDb(env, 'alice');
    await assertFails(getDoc(doc(db, 'secrets/x')));
    await assertFails(setDoc(doc(db, 'secrets/x'), { a: 1 }));
    await assertFails(setDoc(doc(db, 'users/alice/other/x'), { a: 1 }));
    await assertFails(setDoc(doc(db, 'conversations/alice__bob/other/x'), { a: 1 }));
  });

  it('unauthenticated clients can do nothing in chat', async () => {
    await ensureConversation(chatDb(env, 'alice'), 'alice', 'bob');
    const db = anonDb(env);
    await assertFails(setDoc(doc(db, 'conversations/alice__bob/messages/m'), messageData('alice')));
    await assertFails(updateDoc(doc(db, 'conversations/alice__bob'), new FieldPath('typing', 'alice'), serverTimestamp()));
    await assertFails(setDoc(doc(db, 'users/alice'), { username: 'alice', lastActive: serverTimestamp() }));
  });

  it('cid helper matches the app scheme', () => {
    assert.equal(cidOf('bob', 'alice'), 'alice__bob');
  });
});
