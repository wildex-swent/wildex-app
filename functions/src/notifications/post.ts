import {
  onDocumentCreatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  FCMTokenData,
  Notification,
  Post,
  User,
  UserFriends,
  UserSettings,
} from "../types";
import {appAction, postChannelId, removeInvalidTokens} from "../index";
import {Timestamp} from "firebase-admin/firestore";

export const postFunction = onDocumentCreatedWithAuthContext(
  {
    document: "posts/{postId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const post = event.data?.data() as Post;
      const userFriends = (
        await admin.firestore()
          .collection("userFriends")
          .doc(post.authorId)
          .get()
      ).data() as UserFriends;

      // Map to track which tokens belong to which user
      const friendTokenMap = new Map<string, string[]>();

      await Promise.all(
        userFriends.friendsId.map(async (friendId) => {
          const tokenData = (
            await admin.firestore()
              .collection("userTokens")
              .doc(friendId)
              .get()
          ).data() as FCMTokenData;

          if (tokenData?.tokens?.length) {
            friendTokenMap.set(friendId, tokenData.tokens);
          }
        })
      );
      const author = (
        await admin.firestore().collection("users").doc(post.authorId).get()
      ).data() as User;

      const now: Timestamp = admin.firestore.Timestamp.now();

      // Send notifications and clean up invalid tokens per user
      const sendPromises: Promise<void>[] = [];
      for (const [friendId, tokens] of friendTokenMap.entries()) {
        const notification: Notification = {
          notificationId: admin.firestore()
            .collection("notifications")
            .doc()
            .id,
          targetId: friendId,
          authorId: post.authorId,
          title: `${author.username} shared a new post.`,
          body: post.description ? post.description : "",
          route: `post_details/${post.postId}`,
          read: false,
          date: now,
        };
        await admin.firestore()
          .collection("notifications")
          .doc(notification.notificationId)
          .set(notification);

        const userSettings = (
          await admin.firestore()
            .collection("userSettings")
            .doc(friendId)
            .get()
        ).data() as UserSettings;

        if (userSettings?.enableNotifications) {
          const messages = tokens.map((token) => ({
            token,
            android: {
              notification: {
                title: `${author.username} shared a new post.`,
                body: post.description ? post.description : "",
                clickAction: appAction,
                channelId: postChannelId,
                tag: `post_${post.postId}`,
                image: post.pictureURL,
              },
            },
            data: {
              path: `post_details/${post.postId}`,
              notificationId: notification.notificationId,
            },
          }));

          sendPromises.push(
            admin.messaging().sendEach(messages).then((response) =>
              removeInvalidTokens(friendId, response, tokens)
            )
          );
        }
      }
      return sendPromises;
    } catch (error) {
      logger.error(
        "Post Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);
