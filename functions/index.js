const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.notificationSender = functions.database.ref('/Dialogs/{dialogId}/lastMessage').onWrite(async (change, context) => {
    if (!change.after.exists()) {
        return null;
    }
    console.log(change.before.val());
    console.log(change.after.val());
    console.log('__');

    const dialogSnapshot = await change.after.ref.parent.once("value");
    const lastMessage = change.after.val();
    const payload = {
        data: {
            content: lastMessage.fileType || lastMessage.content,
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