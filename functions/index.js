const functions = require('firebase-functions');
const admin = require('firebase-admin');
const _ = require('lodash');
admin.initializeApp();

const VOICE_CALLING_TAG = 'VOICE_CALLING_TAG';
const NOTIFICATION_TAG = 'NOTIFICATION_TAG';

exports.voipNotifier = functions.database.ref('/Users/{userId}/voiceCall').onWrite(async (change, context) => {

    // State of the user which initiator of call else will be port for connection (for java server)
    const CALLING_STATE = "calling...";

    if (!change.after.exists() || change.after.val() === CALLING_STATE) {
        return null;
    }

    const notificationPayload = {
        data: {
            privateRoomPort: change.after.val(),
            tag: VOICE_CALLING_TAG
        }
    };
    const registrationId = await change.after.ref.parent.once("value")
        .then(res => res.val().registrationTokenId);
    return admin.messaging().sendToDevice(registrationId, notificationPayload);

});

exports.notificationSender = functions.database.ref('/Dialogs/{dialogId}/lastMessage').onWrite(async (change, context) => {
    if (!change.after.exists()) {
        return null;
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
        return null;
    }

    console.log(change.before.val());
    console.log(change.after.val());
    console.log('__');

    const dialogSnapshot = await change.after.ref.parent.once("value")
        .then(res => res.val());
    const lastMessage = change.after.val();
    const payload = {
        data: {
            content: lastMessage.fileType || lastMessage.content,
            tag: NOTIFICATION_TAG
        }
    };
    const fetchSpeakersRegIds = [];

    for (let speaker in dialogSnapshot.speakers) {
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
    return admin.messaging().sendToDevice(registrationTokens, payload);
});