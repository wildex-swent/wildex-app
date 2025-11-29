import {Timestamp} from "firebase-admin/firestore";

export interface Animal {
  animalId: string;
  pictureURL: string;
  name: string;
  species: string;
  description: string;
}

export interface FriendRequest {
  senderId: string;
  receiverId: string;
}

export interface Notification {
  notificationId: string;
  targetId: string;
  authorId: string;
  title: string;
  body: string;
  route: string;
  isRead: boolean;
  date: Timestamp;
}

export type NotificationType =
  | "POST"
  | "LIKE"
  | "COMMENT"
  | "FRIEND_REQUEST_ACCEPTED"
  | "FRIEND_REQUEST_RECEIVED"
  | "REPORT_IS_ASSIGNED"
  | "REPORT_IS_RESOLVED";

export type NotificationChannelType =
  | "POSTS"
  | "LIKES"
  | "COMMENTS"
  | "FRIEND_REQUESTS"
  | "REPORTS";

export interface Report {
  reportId: string;
  imageURL: string;
  location: Location;
  date: string;
  description: string;
  authorId: string;
  assigneeId?: string;
}

export interface Comment {
  commentId: string;
  parentId: string;
  authorId: string;
  text: string;
  date: string;
  tag: CommentTag;
}

export type CommentTag = "POST_COMMENT" | "REPORT_COMMENT";

export interface Like {
  likeId: string;
  postId: string;
  userId: string;
}

export interface Post {
  postId: string;
  authorId: string;
  pictureURL: string;
  location?: Location;
  description?: string;
  date: string;
  animalId: string;
  likesCount: number;
  commentsCount: number;
}

export interface SimpleUser {
  userId: string;
  username: string;
  profilePictureURL: string;
  userType: UserType;
}

export interface User {
  userId: string;
  username: string;
  name: string;
  surname: string;
  bio: string;
  profilePictureURL: string;
  userType: UserType;
  creationDate: string;
  country: string;
}

export interface UserAnimals {
  userId: string;
  animalsId: string[];
  animalsCount: number;
}

export type UserType = "REGULAR" | "PROFESSIONAL";

export interface Location {
  latitude: number;
  longitude: number;
  name?: string;
}

export interface FCMTokenData {
    tokens: string[];
}

export interface UserFriends {
  userId: string;
  friendsId: string[];
  friendsCount: number;
}

