import {
  onDocumentCreatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  FCMTokenData,
  Notification,
  Post,
  Report,
  User,
  Comment,
  UserSettings,
} from "../types";
import {appAction, commentChannelId, removeInvalidTokens} from "../index";

export const commentFunction = onDocumentCreatedWithAuthContext(
  {
    document: "comments/{commentId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const commentData = event.data?.data() as Comment;
      const fromUser = (
        await admin.firestore().collection("users")
          .doc(commentData.authorId)
          .get()
      ).data() as User;

      const isPostComment = commentData.tag === "POST_COMMENT";

      const collection = isPostComment ? "posts" : "reports";
      const parentDoc = await admin.firestore().collection(collection)
        .doc(commentData.parentId)
        .get();
      const parentData = parentDoc.data() as Post | Report;

      if (commentData.authorId === parentData.authorId) {
        return null;
      }

      const tokenData = (
        await admin.firestore()
          .collection("userTokens")
          .doc(parentData.authorId)
          .get()
      ).data() as FCMTokenData | undefined;

      const detailPath = isPostComment ?
        `post_details/${(parentData as Post).postId}` :
        `report_details/${(parentData as Report).reportId}`;

      const bodySuffix = isPostComment ? " post." : " report.";

      const notification: Notification = {
        notificationId: admin.firestore().collection("notifications").doc().id,
        targetId: parentData.authorId,
        authorId: commentData.authorId,
        title: `${fromUser.username} commented on your${bodySuffix}`,
        body: commentData.text,
        route: detailPath,
        read: false,
        date: admin.firestore.Timestamp.now(),
      };

      // Create notification document in Firestore
      await admin.firestore()
        .collection("notifications")
        .doc(notification.notificationId)
        .set(notification);

      const userSettings = (
        await admin.firestore()
          .collection("userSettings")
          .doc(parentData.authorId)
          .get()
      ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.log("Comment Notifications: " +
            "No tokens or notifications disabled");
        return null;
      }

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${fromUser.username} commented on your${bodySuffix}`,
            body: commentData.text,
            clickAction: appAction,
            channelId: commentChannelId,
            tag: `comment_${commentData.commentId}`,
          },
        },
        data: {
          path: detailPath,
          notificationId: notification.notificationId,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(parentData.authorId, response, tokenData.tokens)
      );
    } catch (error) {
      logger.error("Comment Notifications error\n", error);
      return null;
    }
  }
);
