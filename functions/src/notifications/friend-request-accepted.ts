import {onDocumentDeletedWithAuthContext} from "firebase-functions/firestore";
import * as admin from "firebase-admin";
import {
  FCMTokenData,
  FriendRequest,
  Notification,
  User,
  UserSettings,
} from "../types";
import {appAction, friendChannelId, removeInvalidTokens} from "../index";
import {logger} from "firebase-functions";

export const friendAcceptedFunction = onDocumentDeletedWithAuthContext(
  {
    document: "friendRequests/{requestId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const requestData = event.data?.data() as FriendRequest;

      const tokenData = (
                await admin
                  .firestore()
                  .collection("userTokens")
                  .doc(requestData.senderId)
                  .get()
            ).data() as FCMTokenData;

      const toUser = (
                await admin
                  .firestore()
                  .collection("users")
                  .doc(requestData.receiverId)
                  .get()
            ).data() as User;

      const notification: Notification = {
        notificationId: admin.firestore().collection("notifications").doc().id,
        targetId: requestData.senderId,
        authorId: requestData.receiverId,
        title: `${toUser.username} accepted your friend request.`,
        body: "",
        route: `friend_screen/${requestData.senderId}`,
        read: false,
        date: admin.firestore.Timestamp.now(),
      };

      // Create notification document in Firestore
      await admin.firestore()
        .collection("notifications")
        .doc(notification.notificationId)
        .set(notification);

      const userSettings = (
                await admin
                  .firestore()
                  .collection("userSettings")
                  .doc(requestData.senderId)
                  .get()
            ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.info("Friend Request Accepted Notifications: " +
                    "No tokens or notifications disabled");
        return null;
      }

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${toUser.username} accepted your friend request.`,
            clickAction: appAction,
            channelId: friendChannelId,
            tag: `friend_request_${requestData.receiverId}`,
          },
        },
        data: {
          path: `friend_screen/${requestData.senderId}`,
          notificationId: notification.notificationId,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(requestData.senderId, response, tokenData.tokens)
      );
    } catch (error) {
      return null;
    }
  }
);
