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
  Post,
  Report,
  User,
  UserFriends,
} from "./types";

admin.initializeApp();

const appAction = "OPEN_MAIN";

const postNotificationTitle = "New Post";
const likeNotificationTitle = "New Like";
const commentNotificationTitle = "New Comment";
const friendRequestNotificationTitle = "New Friend Request";
const friendRequestAcceptedNotificationTitle = "Friend Request Accepted";
const reportAssignedNotificationTitle = "Report Assigned";
const reportResolvedNotificationTitle = "Report Resolved";

const postChannelId = "post_channel";
const likeChannelId = "like_channel";
const commentChannelId = "comment_channel";
const friendRequestChannelId = "friend_request_channel";
const reportChannelId = "report_channel";
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
        await admin.firestore()
          .collection("userFriends")
          .doc(post.authorId)
          .get()
      ).data() as UserFriends;

      const friendTokens = (
        await Promise.all(
          userFriends.friendsId.map(async (friendId) => {
            const tokenData = (
              await admin.firestore()
                .collection("userTokens")
                .doc(friendId)
                .get()
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
            title: postNotificationTitle,
            body: getNotificationBody("POST", user.username),
            clickAction: appAction,
            channelId: postChannelId,
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
            title: friendRequestNotificationTitle,
            body: getNotificationBody(
              "FRIEND_REQUEST_RECEIVED",
              fromUser.username
            ),
            clickAction: appAction,
            channelId: friendRequestChannelId,
          },
        },
        data: {
          path: `friend_screen/${requestData.receiverId}`,
        },
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

exports.sendFriendNotifications = onDocumentDeletedWithAuthContext(
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
            title: friendRequestAcceptedNotificationTitle,
            body: getNotificationBody(
              "FRIEND_REQUEST_ACCEPTED",
              toUser.username
            ),
            clickAction: appAction,
            channelId: friendRequestChannelId,
          },
        },
        data: {path: `friend_screen/${requestData.senderId}`},
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
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
            title: likeNotificationTitle,
            body: getNotificationBody("LIKE", fromUser.username),
            clickAction: appAction,
            channelId: likeChannelId,
          },
        },
        data: {path: `post_details/${postData.postId}`},
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
            title: commentNotificationTitle,
            body: getNotificationBody(
              "COMMENT",
              fromUser.username
            ) + ` ${commentData.tag == "POST_COMMENT" ? "post." : "comment."}`,
            clickAction: appAction,
            channelId: commentChannelId,
          },
        },
        data: {path: `post_details/${postData.postId}`},

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
            title: reportAssignedNotificationTitle,
            body: getNotificationBody(
              "REPORT_IS_ASSIGNED",
              assigneeUser.username
            ),
            clickAction: appAction,
            channelId: reportChannelId,
          },
        },
        data: {path: `report_details/${reportData.reportId}`},
      }));

      return admin.messaging().sendEach(messages);
    } catch (error) {
      return null;
    }
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

      if (!reportData.assigneeId) {
        return null;
      }
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
            title: reportResolvedNotificationTitle,
            body: getNotificationBody(
              "REPORT_IS_RESOLVED",
              assigneeUser.username
            ),
            clickAction: appAction,
            channelId: reportChannelId,
          },
        },
        data: {path: `report_details/${reportData.reportId}`},
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
