const functions = require('firebase-functions');
const admin = require('firebase-admin');
const path = require('path');
const _ = require('lodash');
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
    const payload = {
        data: {
            content: lastMessage.fileType || lastMessage.content,
            tag: NOTIFICATION_TAG
        }
    };
    const options = {
        collapseKey: dialogSnapshot.key
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
    const registrationTokens = [];
    usersSnapshots.forEach(userSnapshot => {
        userSnapshot.forEach(snap => {
            const user = snap.val();

            if (user.uid === lastMessage.who) {
                const {avatarUrl, login, uid} = user;
                payload.data.sender = JSON.stringify({avatarUrl, login, uid});
            }

            const regToken = user.registrationTokenId;
            if (regToken) {
                registrationTokens.push(regToken);
            }
        });
    });
    return admin.messaging().sendToDevice(registrationTokens, payload, options);
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
