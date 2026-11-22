import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { deleteObject, getMetadata, listAll, ref, uploadBytes } from 'firebase/storage';
import { createEnv } from './helpers.js';

const MB = 1024 * 1024;
const jpeg = { contentType: 'image/jpeg' };
const bytes = (n) => new Uint8Array(n);

let env;

before(async () => {
  env = await createEnv({ storage: true });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearStorage();
});

const chat = (uid) => env.authenticatedContext(uid).storage();
const google = (uid) => env.authenticatedContext(uid, { firebase: { sign_in_provider: 'google.com', identities: {} } }).storage();
const anon = () => env.unauthenticatedContext().storage();

async function seedFile(path, contentType = 'image/jpeg') {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await uploadBytes(ref(ctx.storage(), path), bytes(16), { contentType });
  });
}

describe('storage: images/** (artwork uploads)', () => {
  it('any signed-in user uploads an image (Google/phone or chat session), nested names too', async () => {
    await assertSucceeds(uploadBytes(ref(google('firebaseUid1'), 'images/1000000033.jpg'), bytes(1024), jpeg));
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'images/abc.jpg'), bytes(1024), { contentType: 'image/png' }));
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'images/primary:DCIM/Camera/x.jpg.jpg'), bytes(1024), jpeg));
  });

  it('just under 20 MB is allowed; 20 MB is denied', async () => {
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'images/big.jpg'), bytes(20 * MB - 1), jpeg));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/too-big.jpg'), bytes(20 * MB), jpeg));
  });

  it('Android putFile of a file:// uri (no metadata → application/octet-stream) is accepted for image names', async () => {
    const octet = { contentType: 'application/octet-stream' };
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'images/1727000000000.jpg.jpg'), bytes(1024), octet));
    await assertSucceeds(uploadBytes(ref(google('uid1'), 'images/cropped.JPEG'), bytes(1024), octet));
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'images/a.png'), bytes(1024), octet));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/big.jpg'), bytes(20 * MB), octet));
    await assertFails(uploadBytes(ref(anon(), 'images/a.jpg'), bytes(1024), octet));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/a.jpg.html'), bytes(1024), octet));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/a.jpg'), bytes(1024), { contentType: 'text/html' }));
    // chat photos always carry image/jpeg metadata, so octet-stream stays denied there
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/x.jpg'), bytes(1024), octet));
  });

  it('denies unauthenticated uploads, non-images and missing content types', async () => {
    await assertFails(uploadBytes(ref(anon(), 'images/a.jpg'), bytes(1024), jpeg));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/a.html'), bytes(1024), { contentType: 'text/html' }));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/a.bin'), bytes(1024), { contentType: 'application/octet-stream' }));
    await assertFails(uploadBytes(ref(chat('alice'), 'images/noext'), bytes(1024)));
  });

  it('public read of a single file; listing and deleting are denied', async () => {
    await seedFile('images/pub.jpg');
    await assertSucceeds(getMetadata(ref(anon(), 'images/pub.jpg')));
    await assertSucceeds(getMetadata(ref(chat('bob'), 'images/pub.jpg')));
    await assertFails(listAll(ref(anon(), 'images')));
    await assertFails(deleteObject(ref(chat('alice'), 'images/pub.jpg')));
    await assertFails(deleteObject(ref(anon(), 'images/pub.jpg')));
  });
});

describe('storage: chat/{cid}/{uid}/{file}', () => {
  it('the uploader writes an image into its own folder of a conversation it is part of', async () => {
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/0b3c.jpg'), bytes(2048), jpeg));
    await assertSucceeds(uploadBytes(ref(chat('bob'), 'chat/alice__bob/bob/9f1e.jpg'), bytes(2048), jpeg));
    await assertSucceeds(uploadBytes(ref(chat('john.doe'), 'chat/john.doe__mary_k/john.doe/1.jpg'), bytes(2048), jpeg));
    await assertSucceeds(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/max.jpg'), bytes(10 * MB - 1), jpeg));
  });

  it("denies writing another user's folder, a conversation it is not part of, or as a non-chat session", async () => {
    await assertFails(uploadBytes(ref(chat('bob'), 'chat/alice__bob/alice/x.jpg'), bytes(2048), jpeg));
    await assertFails(uploadBytes(ref(chat('carol'), 'chat/alice__bob/carol/x.jpg'), bytes(2048), jpeg));
    await assertFails(uploadBytes(ref(chat('ali'), 'chat/alice__bob/ali/x.jpg'), bytes(2048), jpeg));
    await assertFails(uploadBytes(ref(google('alice'), 'chat/alice__bob/alice/x.jpg'), bytes(2048), jpeg));
    await assertFails(uploadBytes(ref(anon(), 'chat/alice__bob/alice/x.jpg'), bytes(2048), jpeg));
  });

  it('denies non-images and files of 10 MB or more', async () => {
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/x.mp4'), bytes(2048), { contentType: 'video/mp4' }));
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/x.pdf'), bytes(2048), { contentType: 'application/pdf' }));
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/big.jpg'), bytes(10 * MB), jpeg));
  });

  it('parties read; outsiders, anonymous, listing and deleting are denied', async () => {
    await seedFile('chat/alice__bob/alice/p.jpg');
    await assertSucceeds(getMetadata(ref(chat('alice'), 'chat/alice__bob/alice/p.jpg')));
    await assertSucceeds(getMetadata(ref(chat('bob'), 'chat/alice__bob/alice/p.jpg')));
    await assertFails(getMetadata(ref(chat('carol'), 'chat/alice__bob/alice/p.jpg')));
    await assertFails(getMetadata(ref(anon(), 'chat/alice__bob/alice/p.jpg')));
    await assertFails(listAll(ref(chat('alice'), 'chat/alice__bob/alice')));
    await assertFails(deleteObject(ref(chat('alice'), 'chat/alice__bob/alice/p.jpg')));
  });

  it('wrong depth under chat/ is denied', async () => {
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice.jpg'), bytes(10), jpeg));
    await assertFails(uploadBytes(ref(chat('alice'), 'chat/alice__bob/alice/sub/x.jpg'), bytes(10), jpeg));
  });
});

describe('storage: everything else', () => {
  it('other paths are denied', async () => {
    await seedFile('private/secret.jpg');
    await assertFails(getMetadata(ref(chat('alice'), 'private/secret.jpg')));
    await assertFails(uploadBytes(ref(chat('alice'), 'avatars/alice.jpg'), bytes(10), jpeg));
    await assertFails(uploadBytes(ref(chat('alice'), 'x.jpg'), bytes(10), jpeg));
  });
});
