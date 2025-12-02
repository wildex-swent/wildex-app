import {
  onDocumentCreatedWithAuthContext,
  onDocumentDeletedWithAuthContext,
  onDocumentUpdatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  Comment,
  FCMTokenData,
  FriendRequest,
  Like,
  Notification,
  Post,
  Report,
  User,
  UserFriends,
} from "./types";
import {BatchResponse} from "firebase-admin/messaging";

admin.initializeApp();

const appAction = "OPEN_MAIN";

const postChannelId = "post_channel";
const likeChannelId = "like_channel";
const commentChannelId = "comment_channel";
const friendRequestChannelId = "friend_request_channel";
const reportChannelId = "report_channel";

exports.sendPostNotifications = onDocumentCreatedWithAuthContext(
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

      const notifications: Notification[] = userFriends.friendsId.map(
        (friendId) => (
          {
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
            date: admin.firestore.Timestamp.now(),
          }
        )
      );

      // Create notification documents in Firestore
      await Promise.all(
        notifications.map((notification) =>
          admin.firestore()
            .collection("notifications")
            .doc(notification.notificationId)
            .set(notification)
        )
      );

      // Send notifications and clean up invalid tokens per user
      const sendPromises: Promise<void>[] = [];
      for (const [friendId, tokens] of friendTokenMap.entries()) {
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
            authorId: post.authorId,
          },
        }));


        sendPromises.push(
          admin.messaging().sendEach(messages).then((response) =>
            removeInvalidTokens(friendId, response, tokens)
          )
        );
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

exports.sendFriendRequestNotifications = onDocumentCreatedWithAuthContext(
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

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${fromUser.username} sent you a friend request.`,
            clickAction: appAction,
            channelId: friendRequestChannelId,
            tag: `friend_request_${requestData.senderId}`,
          },
        },
        data: {
          path: `friend_screen/${requestData.receiverId}`,
          authorId: requestData.senderId,
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

exports.sendFriendAcceptedNotifications = onDocumentDeletedWithAuthContext(
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

      const userFriends = (
        await admin
          .firestore()
          .collection("userFriends")
          .doc(requestData.senderId)
          .get()
      ).data() as UserFriends;

      if (!userFriends.friendsId.includes(requestData.receiverId)) {
        return null;
      }


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

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${toUser.username} accepted your friend request.`,
            clickAction: appAction,
            channelId: friendRequestChannelId,
            tag: `friend_request_${requestData.receiverId}`,
          },
        },
        data: {
          path: `friend_screen/${requestData.senderId}`,
          authorId: requestData.receiverId,
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

exports.sendLikeNotifications = onDocumentCreatedWithAuthContext(
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
          authorId: likeData.userId,
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

exports.sendCommentNotifications = onDocumentCreatedWithAuthContext(
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

      if (!tokenData?.tokens?.length) return null;

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
          authorId: commentData.authorId,
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


exports.sendReportAssignmentNotifications = onDocumentUpdatedWithAuthContext(
  {
    document: "reports/{reportId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const reportData = event.data?.after.data() as Report;

      if (!reportData.assigneeId) {
        logger.log(
          `No notification sent: Report ${reportData.reportId} has no assignee`
        );
        return null;
      }

      if (reportData.assigneeId === reportData.authorId) {
        return null;
      }

      const tokenData = (
        await admin
          .firestore()
          .collection("userTokens")
          .doc(reportData.authorId)
          .get()
      ).data() as FCMTokenData;

      const assigneeUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(reportData.assigneeId)
          .get()
      ).data() as User;

      const notification: Notification = {
        notificationId: reportData.assigneeId! + "_" + reportData.reportId,
        targetId: reportData.authorId,
        authorId: reportData.assigneeId,
        title: `${assigneeUser.username} is assigned to your report.`,
        body: "",
        route: `report_details/${reportData.reportId}`,
        read: false,
        date: admin.firestore.Timestamp.now(),
      };

      // Create notification document in Firestore
      await admin.firestore()
        .collection("notifications")
        .doc(notification.notificationId)
        .set(notification);

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${assigneeUser.username} is assigned to your report.`,
            body: "",
            clickAction: appAction,
            channelId: reportChannelId,
            tag: `report_${reportData.reportId}`,
          },
        },
        data: {
          path: `report_details/${reportData.reportId}`,
          authorId: reportData.assigneeId!,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(reportData.authorId, response, tokenData.tokens)
      );
    } catch (error) {
      return null;
    }
  }
);

exports.sendReportResolutionNotifications = onDocumentDeletedWithAuthContext(
  {
    document: "reports/{reportId}",
    region: "europe-west1",
  },
  async (event) => {
    try {
      const reportData = event.data?.data() as Report;

      const tokenData = (
        await admin
          .firestore()
          .collection("userTokens")
          .doc(reportData.authorId)
          .get()
      ).data() as FCMTokenData;

      if (!reportData.assigneeId) {
        return null;
      }

      if (reportData.assigneeId === reportData.authorId) {
        return null;
      }

      const assigneeUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(reportData.assigneeId)
          .get()
      ).data() as User;

      const notification: Notification = {
        notificationId: admin.firestore().collection("notifications").doc().id,
        targetId: reportData.authorId,
        authorId: reportData.assigneeId,
        title: `${assigneeUser.username} resolved your report.`,
        body: "",
        route: `report_details/${reportData.reportId}`,
        read: false,
        date: admin.firestore.Timestamp.now(),
      };

      // Create notification document in Firestore
      await admin.firestore()
        .collection("notifications")
        .doc(notification.notificationId)
        .set(notification);

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: `${assigneeUser.username} resolved your report.`,
            clickAction: appAction,
            channelId: reportChannelId,
            tag: `report_${reportData.reportId}`,
          },
        },
        data: {
          path: "report",
          authorId: reportData.assigneeId!,
        },
      }));

      return admin.messaging().sendEach(messages).then((response) =>
        removeInvalidTokens(reportData.authorId, response, tokenData.tokens)
      );
    } catch (error) {
      logger.error(
        "Report Resolution Notifications : The following error has occured\n",
        error
      );
    }
    return null;
  }
);


/**
 * Removes invalid FCM tokens from a user's token array in Firestore.
 * Call this after sending notifications to clean up tokens
 * that are no longer valid.
 *
 * @param {string} userId - The user ID whose tokens to check
 * @param {BatchResponse} sendResponse - The response from
 * admin.messaging().sendEach()
 * @param {string[]} tokensSent - The array of tokens
 * that were sent notifications
 */
async function removeInvalidTokens(
  userId: string,
  sendResponse: BatchResponse,
  tokensSent: string[]
): Promise<void> {
  try {
    const invalidTokens: string[] = [];

    // Check each response for invalid token errors
    sendResponse.responses.forEach((response, index) => {
      if (!response.success && response.error) {
        const errorCode = response.error.code;

        // These error codes indicate the token is permanently invalid
        if (
          errorCode === "messaging/registration-token-not-registered" ||
          errorCode === "messaging/invalid-argument" ||
          errorCode === "messaging/invalid-registration-token"
        ) {
          invalidTokens.push(tokensSent[index]);
          logger.info(
            `Invalid token detected for user ${userId}: ${errorCode}`
          );
        }
      }
    });

    // Remove invalid tokens from Firestore if any were found
    if (invalidTokens.length > 0) {
      const tokenDocRef = admin
        .firestore()
        .collection("userTokens")
        .doc(userId);

      await tokenDocRef.update({
        tokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
      });

      logger.info(
        `Removed ${invalidTokens.length} invalid token(s) for user ${userId}`
      );
    }
  } catch (error) {
    logger.error(
      `Error removing invalid tokens for user ${userId}:`,
      error
    );
  }
}
