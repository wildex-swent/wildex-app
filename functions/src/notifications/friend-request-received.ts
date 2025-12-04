import {
  onDocumentCreatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  FCMTokenData,
  FriendRequest,
  Notification,
  User,
  UserSettings,
} from "../types";
import {appAction, friendChannelId, removeInvalidTokens} from "..";

export const friendReceivedFunction = onDocumentCreatedWithAuthContext(
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
          .doc(requestData.receiverId)
          .get()
      ).data() as FCMTokenData;

      const fromUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(requestData.senderId)
          .get()
      ).data() as User;

      const notification: Notification = {
        notificationId: requestData.receiverId + "_" + requestData.senderId,
        targetId: requestData.receiverId,
        authorId: requestData.senderId,
        title: `${fromUser.username} sent you a friend request.`,
        body: "",
        route: `friend_screen/${requestData.receiverId}`,
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
          .doc(requestData.receiverId)
          .get()
      ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.log(
          "Friend Request Notifications: No tokens or notifications disabled"
        );
        return null;
      }

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${fromUser.username} sent you a friend request.`,
            clickAction: appAction,
            channelId: friendChannelId,
            tag: `friend_request_${requestData.senderId}`,
          },
        },
        data: {
          path: `friend_screen/${requestData.receiverId}`,
          notificationId: notification.notificationId,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(requestData.receiverId, response, tokenData.tokens)
      );
    } catch (error) {
      logger.error(
        "Friend Request Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);
