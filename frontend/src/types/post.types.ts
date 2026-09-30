export type PostType =
    | "LEARNING_VIDEO"
    | "SOCIAL_POST"
    | "QUESTION_APPROVED_NOTIFICATION"
    | "LEADERBOARD_HONOR";

export type PostVisibility = "PUBLIC" | "FRIENDS" | "ONLY_ME";

// Author/User DTO - tuân thủ quy ước tên ID tường minh (userId)
export interface AuthorDto {
    userId: number;
    fullName: string;
    avatarUrl?: string;
    frameUrl?: string;
    role?: "STUDENT" | "LECTURER" | "ADMIN";
    schoolFaculty?: string;
}

export interface DeletePostResponseDto {
    postId: number;
}

export interface MediaItem {
    objectKey: string;
    url: string;
    mediaType: string;
    width?: number;
    height?: number;
    size?: number;
    duration?: number;
    name?: string;
    thumbnailUrl?: string;
}

export interface PostResponseDto {
    postId: number;
    content: string;
    postType: PostType;
    mediaUrls?: MediaItem[];
    visibility: PostVisibility;
    author: AuthorDto;

    reactCount: number;
    commentCount: number;
    reactedByMe: boolean;

    createdAt: string;
    updatedAt?: string;

    // Giảng viên ghi chú chuyên môn (Academic Feature)
    lecturerNote?: string;
    lecturerNoteAddedAt?: string;
    notedLecturer?: AuthorDto;

    // Gắn thẻ môn học
    subjectId?: number;
    subjectName?: string;
    subjectCode?: string;

    sessionId?: number;
}

export interface CommentResponseDto {
    commentId: number;
    targetType: string;
    targetId: number;
    parentCommentId?: number;

    content: string;
    mediaUrl?: string;
    author: AuthorDto;

    createdAt: string;
    hidden?: boolean;

    replyCount?: number;
    replies?: CommentResponseDto[];
}

export interface SubjectItem {
    id: number;
    code: string;
    name: string;
    postCount?: number;
}

export interface CreatePostRequest {
    content: string;
    mediaUrls?: MediaItem[];
    visibility: PostVisibility;
    subjectId?: number;
    postType?: PostType;
}

export interface UpdatePostRequest {
    content?: string;
    mediaUrls?: MediaItem[];
    visibility: PostVisibility;
}

export interface UpdatePostResponseDto {
    postId: number;
    updatedAt: string;
}

export interface UpdateLecturerNoteRequest {
    content: string;
}

export interface VideoPostItemDto {
    postId: number;
    content: string;
    visibility: PostVisibility;
    reactCount: number;
    commentCount: number;
    mediaUrls?: MediaItem[];
    author: AuthorDto;
    createdAt: string;
    updatedAt?: string;
    subjectId?: number;
    subjectName?: string;
    subjectCode?: string;
}

