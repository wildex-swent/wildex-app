import {
  onDocumentCreatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  FCMTokenData,
  Like,
  Notification,
  Post,
  User,
  UserSettings,
} from "../types";
import {appAction, likeChannelId, removeInvalidTokens} from "..";

export const likeFunction = onDocumentCreatedWithAuthContext(
  {
    document: "likes/{likeId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const likeData = event.data?.data() as Like;

      const postDoc = await admin
        .firestore()
        .collection("posts")
        .doc(likeData.postId)
        .get();
      const postData = postDoc.data() as Post;

      if (likeData.userId === postData.authorId) {
        return null;
      }

      const tokenData = (
        await admin
          .firestore()
          .collection("userTokens")
          .doc(postData.authorId)
          .get()
      ).data() as FCMTokenData;

      const fromUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(likeData.userId)
          .get()
      ).data() as User;

      const notification: Notification = {
        notificationId: likeData.userId + "_" + likeData.postId,
        targetId: postData.authorId,
        authorId: likeData.userId,
        title: `${fromUser.username} liked your post.`,
        body: "",
        route: `post_details/${postData.postId}`,
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
          .doc(postData.authorId)
          .get()
      ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.log("Like Notifications: No tokens or notifications disabled");
        return null;
      }

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${fromUser.username} liked your post.`,
            body: "",
            clickAction: appAction,
            channelId: likeChannelId,
            tag: `liker_${likeData.userId}`,
          },
        },
        data: {
          path: `post_details/${postData.postId}`,
          notificationId: notification.notificationId,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(postData.authorId, response, tokenData.tokens)
      );
    } catch (error) {
      logger.error(
        "Like Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);
