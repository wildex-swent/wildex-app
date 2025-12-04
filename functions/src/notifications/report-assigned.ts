import {
  onDocumentUpdatedWithAuthContext,
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

export const reportAssignedFunction = onDocumentUpdatedWithAuthContext(
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

      const userSettings = (
        await admin
          .firestore()
          .collection("userSettings")
          .doc(reportData.authorId)
          .get()
      ).data() as UserSettings;

      if (!tokenData?.tokens?.length || !userSettings?.enableNotifications) {
        logger.log(
          "Report Assignment Notifications: No tokens or notifications disabled"
        );
        return null;
      }

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
          notificationId: notification.notificationId,
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
