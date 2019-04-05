const functions = require('firebase-functions');
const admin = require('firebase-admin');
const path = require('path');
const os = require('os');
const fs = require('fs');
const _ = require('lodash');
const uuid = require('uuid');
const sharp = require('sharp');
const Busboy = require('busboy');
admin.initializeApp();

const VOICE_CALLING_TAG = 'VOICE_CALLING_TAG';
const NOTIFICATION_TAG = 'NOTIFICATION_TAG';

exports.voipNotifier = functions.database.ref('/Users/{userId}/voiceCall').onWrite(async (change, context) => {

    // State of the user which initiator of call else will be port for connection (for java server)
    const CALLING_STATE = "calling...";

    if (!change.after.exists() || change.after.val() === CALLING_STATE) {
        return Promise.reject(new Error('Incorrect calling state'));
    }

    const notificationPayload = {
        data: {
            privateRoomPort: change.after.val(),
            tag: VOICE_CALLING_TAG
        }
    };
    const options = {
        priority: "high",
        timeToLive: 25
    };

    const registrationId = await change.after.ref.parent.once("value")
        .then(res => res.val().registrationTokenId);
    return admin.messaging().sendToDevice(registrationId, notificationPayload, options);

});

exports.notificationSender = functions.database.ref('/Dialogs/{dialogId}/lastMessage').onWrite(async (change, context) => {
    if (!change.after.exists()) {
        return Promise.reject(new Error('Delete event will be ignored'));
    }

    const prevSnapshot = {
        ...change.before.val(),
        read: null
    };
    const currentSnapshot = {
        ...change.after.val(),
        read: null
    };

    if (_.isEqual(prevSnapshot, currentSnapshot)) {
        return Promise.reject(new Error('Reading changes will be ignored'));
    }

    console.log(change.before.val());
    console.log(change.after.val());
    console.log('__');

    const dialogSnapshot = await change.after.ref.parent.once("value");
    const lastMessage = change.after.val();
    const messageContent = lastMessage.fileType || lastMessage.content;
    const message = {
        data: {
            content: messageContent,
            tag: NOTIFICATION_TAG
        },
        tokens: [],
        android: {
            collapseKey: dialogSnapshot.key,
            priority: "high"
        },
        webpush: {
            headers: {
                Urgency: "high"
            },
            notification: {
                body: messageContent,
                // requireInteraction: "true",
                // badge: "/badge-icon.png"
            }
        }
    };

    const fetchSpeakersRegIds = [];

    for (let speaker in dialogSnapshot.val().speakers) {
        fetchSpeakersRegIds.push(
            admin.database()
                .ref('Users')
                .orderByChild('uid')
                .equalTo(speaker)
                .once('value')
        );
    }

    const usersSnapshots = await Promise.all(fetchSpeakersRegIds);
    usersSnapshots.forEach(userSnapshot => {
        userSnapshot.forEach(snap => {
            const user = snap.val();

            if (user.uid === lastMessage.who) {
                const {avatarUrl, login, uid} = user;
                message.data.sender = JSON.stringify({avatarUrl, login, uid});
            }

            const { registrationTokenId, webNotificationToken } = user;
            if (registrationTokenId) {
                message.tokens.push(registrationTokenId);
            }
            if (webNotificationToken) {
                message.tokens.push(webNotificationToken);
            }
        });
    });

    return admin.messaging().sendMulticast(message);
});

exports.messageResourceCascadeDelete = functions.database.ref('/Messages/{dialogId}/{messageId}').onDelete((change, context) => {
    const deletedItem = change.val();

    if (deletedItem.mediaFile) {
        let { relativePath, bucket } = deletedItem.mediaFile;
        const fileName = path.basename(relativePath);
        const fileDirname = path.dirname(relativePath);
        console.log(path.join(fileDirname, `thumb_${fileName}`));
        bucket = admin.storage().bucket(bucket);
        bucket.file(relativePath).delete(res => console.log(res));
        bucket.file(path.join(fileDirname, `thumb_${fileName}`)).delete(res => console.log(res));
        return Promise.resolve();
    }
    return Promise.reject(new Error('Data without mediaFile will be ignored'));
});

exports.lastMessage = functions.database.ref('/Messages/{dialogId}/{messageId}').onWrite( async (change, context) => {
    const dialogId = context.params.dialogId;
    let lastMessage = change.after.val();

    if (!change.after.exists()) {
        const lastMsgSnapshot = await admin.database()
            .ref(`/Messages/${dialogId}`)
            .limitToLast(1)
            .once('value');
        lastMsgSnapshot.forEach(lastMsg => lastMessage = lastMsg.val());

        if (!lastMessage) {
            return admin.database()
                .ref(`/Dialogs/${dialogId}`)
                .remove();
        }
    }

    return admin.database()
        .ref(`/Dialogs/${dialogId}/lastMessage`)
        .set(lastMessage);
});

/*
exports.generateThumbnail = functions.storage.object().onFinalize(async object => {

    const contentType = object.contentType;
    const filePath = object.name;

    if (!contentType.startsWith('image/')) {
        return Promise.reject(new Error('Data type is not image'));
    }

    const fileName = path.basename(filePath);
    if (fileName.startsWith('thumb_')) {
        return Promise.reject(new Error('Data already resized'));
    }

    const metadata = { contentType };
    const fileBucket = object.bucket;
    const bucket = admin.storage().bucket(fileBucket);

    const tmpFilePath = path.join(os.tmpdir(), fileName);
    const tmpThumbFilePath = path.join(os.tmpdir(), `thumb_${fileName}`);
    const thumbFilePath = path.join(path.dirname(filePath), `thumb_${fileName}`);

    await bucket.file(filePath).download({destination: tmpFilePath});

    await sharp(tmpFilePath)
        .resize(null, 450)
        .toFile(tmpThumbFilePath);

    await bucket.upload(tmpThumbFilePath, {destination: thumbFilePath, metadata});

    fs.unlink(tmpFilePath, (err) => err && console.error(err));
    fs.unlink(tmpThumbFilePath, (err) => err && console.error(err));
    return Promise.resolve('Ok');
});
*/

exports.uploadImage = functions.https.onRequest(async (request, response) => {

    const busboy = new Busboy({ headers: request.headers });
    const fetchFile = new Promise((resolve, reject) => {
        busboy.on('file', (fieldname, file, filename, encoding, mimetype) => {
            file.on('data', data => {
                console.log('read');
                resolve({file: data, mimetype});
            })
        });
    });
    busboy.end(request.rawBody);

    const fetchedFile = await fetchFile;
    const {file, mimetype} = fetchedFile;

    if (!mimetype.startsWith('image/')) {
        return Promise.reject(new Error('Data type is not image'));
    }

    const fileExtension = mimetype.split('/')[1];
    const fileName = `${uuid.v4()}.${fileExtension}`;
    const fileDir = '/testDir';

    const bucket = admin.storage().bucket();

    const thumbFilePath = `${fileDir}/thumb_${fileName}`;
    const tmpThumbFilePath = path.join(os.tmpdir(), `thumb_${fileName}`);

    await sharp(file)
        .resize(null, 450)
        .toFile(tmpThumbFilePath);
    console.log('rseized');
    await bucket.upload(tmpThumbFilePath, {destination: thumbFilePath })
    console.log('saved');

    fs.unlink(tmpThumbFilePath, (err) => err && console.error(err));
    return response.status(200).send();
});