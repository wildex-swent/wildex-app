import {
  onDocumentCreatedWithAuthContext,
  onDocumentDeletedWithAuthContext,
  onDocumentUpdatedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { logger } from "firebase-functions";
import { Comment, FCMTokenData, FriendRequest, Like, Post, Report, User, UserFriends } from "./types";

admin.initializeApp();

/**
 * Maps notification types to notification bodies.
 * @param {string} type - Notification type
 * @param {string} username - Username of the actor
 * @return {string} Notification body string
 */
function getNotificationBody(type: string, username: string): string {
  const bodies: { [key: string]: string } = {
    POST: `${username} shared a new post.`,
    LIKE: `${username} liked your post.`,
    COMMENT: `${username} commented on your`,
    FRIEND_REQUEST_ACCEPTED: `${username} accepted your friend request.`,
    FRIEND_REQUEST_RECEIVED: `${username} sent you a friend request.`,
    REPORT_IS_ASSIGNED: `${username} is assigned to your report.`,
    REPORT_IS_RESOLVED: `${username} resolved your report.`,
  };
  return bodies[type] || "You have a new notification.";
}


exports.sendPostNotifications = onDocumentCreatedWithAuthContext(
  {
    document: "posts/{postId}",
    region: "europe-west6",
  },
  async (event) => {
    try {
      const post = event.data?.data() as Post;
      const userFriends = (
        await admin.firestore().collection("userFriends").doc(post.authorId).get()
      ).data() as UserFriends;

      const friendTokens = (
        await Promise.all(
          userFriends.friendsId.map(async (friendId) => {
            const tokenData = (
              await admin.firestore().collection("userTokens").doc(friendId).get()
            ).data() as FCMTokenData;
            return tokenData ? tokenData.tokens : [];
          })
        )
      ).flat();

      const user = (
        await admin.firestore().collection("users").doc(post.authorId).get()
      ).data() as User;

      const messages = friendTokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "New Post",
            body: getNotificationBody("POST", user.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "0",
          },
        },
        data: {
          path: `post_details/${post.postId}`,
        },
      }));

      return admin.messaging().sendEach(messages);
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
    region: "europe-west6",
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

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "New Friend Request",
            body: getNotificationBody("FRIEND_REQUEST_RECEIVED", fromUser.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "3",
          },
        },
        data: { path: `friend_screen/${requestData.receiverId}` },
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      logger.error(
        "Friend Request Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);

exports.sendFriendAcceptedOrRefusedNotifications = onDocumentDeletedWithAuthContext(
  {
    document: "friendRequests/{requestId}",
    region: "europe-west6",
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
        logger.log(
          `No notification sent: ${requestData.receiverId} is not a friend of ${requestData.senderId}`
        );
        return null;
      }


      const toUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(requestData.receiverId)
          .get()
      ).data() as User;

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "Friend Request Accepted",
            body: getNotificationBody("FRIEND_REQUEST_ACCEPTED", toUser.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "3",
          },
        },
        data: { path: `friend_screen/${requestData.senderId}` },
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      logger.error(
        "Friend Request Accepted Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);

exports.sendLikeNotifications = onDocumentCreatedWithAuthContext(
  {
    document: "likes/{likeId}",
    region: "europe-west6",
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

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "New Like",
            body: getNotificationBody("LIKE", fromUser.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "1",
          },
        },
        data: { path: `post_details/${postData.postId}` },
      }));

      return admin.messaging().sendEach(messages);
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
    region: "europe-west6",
  },
  async (event) => {
    try {
      const commentData = event.data?.data() as Comment;

      const postDoc = await admin
        .firestore()
        .collection("posts")
        .doc(commentData.parentId)
        .get();
      const postData = postDoc.data() as Post;

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
          .doc(commentData.authorId)
          .get()
      ).data() as User;

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "New Comment",
            body: getNotificationBody("COMMENT", fromUser.username) + ` ${commentData.tag == "POST_COMMENT" ? "post." : "comment."}`,
            clickAction: "android.intent.action.MAIN",
            channelId: "2",
          },
        },
        data: { path: `post_details/${postData.postId}` },
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      logger.error(
        "Comment Notifications : The following error has occured\n",
        error
      );
      return null;
    }
  }
);

exports.sendReportAssignmentNotifications = onDocumentUpdatedWithAuthContext(
  {
    document: "reports/{reportId}",
    region: "europe-west6",
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

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "Report Assigned",
            body: getNotificationBody("REPORT_IS_ASSIGNED", assigneeUser.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "4",
          },
        },
        data: { path: `report_details/${reportData.reportId}` },
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      logger.error(
        "Report Assignment Notifications : The following error has occured\n",
        error
      );
    }
    return null;
  }
);

exports.sendReportResolutionNotifications = onDocumentDeletedWithAuthContext(
  {
    document: "reports/{reportId}",
    region: "europe-west6",
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

      const assigneeUser = (
        await admin
          .firestore()
          .collection("users")
          .doc(reportData.assigneeId!)
          .get()
      ).data() as User;

      const messages = tokenData.tokens.map((token) => ({
        token,
        android: {
          notification: {
            title: "Report Resolved",
            body: getNotificationBody("REPORT_IS_RESOLVED", assigneeUser.username),
            clickAction: "android.intent.action.MAIN",
            channelId: "4",
          },
        },
        data: { path: `report_details/${reportData.reportId}` },
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      logger.error(
        "Report Resolution Notifications : The following error has occured\n",
        error
      );
    }
    return null;
  }
);
