import {
  onDocumentDeletedWithAuthContext,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {
  FCMTokenData,
  Notification,
  Report,
  User,
  UserSettings,
} from "../types";
import {appAction, reportChannelId, removeInvalidTokens} from "../index";

export const reportResolvedFunction = onDocumentDeletedWithAuthContext(
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

      if (!reportData.assigneeId ||
        (reportData.assigneeId === reportData.authorId)) {
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

      const userSettings = (
        await admin
          .firestore()
          .collection("userSettings")
          .doc(reportData.authorId)
          .get()
      ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.log(
          "Report Resolution Notifications: No tokens or notifications disabled"
        );
        return null;
      }

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
          notificationId: notification.notificationId,
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
