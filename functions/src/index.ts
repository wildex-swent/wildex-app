
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";
import {BatchResponse} from "firebase-admin/messaging";
import {postFunction} from "./notifications/post";
import {friendReceivedFunction} from "./notifications/friend-request-received";
import {friendAcceptedFunction} from "./notifications/friend-request-accepted";
import {likeFunction} from "./notifications/like";
import {commentFunction} from "./notifications/comment";
import {reportAssignedFunction} from "./notifications/report-assigned";
import {reportResolvedFunction} from "./notifications/report-resolved";

admin.initializeApp();

export const appAction = "OPEN_MAIN";

export const postChannelId = "post_channel";
export const likeChannelId = "like_channel";
export const commentChannelId = "comment_channel";
export const friendChannelId = "friend_channel";
export const reportChannelId = "report_channel";

exports.sendPostNotifications = postFunction;
exports.sendLikeNotifications = likeFunction;
exports.sendCommentNotifications = commentFunction;

exports.sendFriendRequestNotifications = friendReceivedFunction;
exports.sendFriendAcceptedNotifications = friendAcceptedFunction;

exports.sendReportAssignmentNotifications = reportAssignedFunction;
exports.sendReportResolutionNotifications = reportResolvedFunction;


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
export async function removeInvalidTokens(
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
