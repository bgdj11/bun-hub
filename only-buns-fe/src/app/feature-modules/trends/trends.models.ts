export interface ImageDTO {
  path: string;
}

export interface LocationDTO {
  country?: string;
  city?: string;
  address?: string;
  number?: number;
  latitude?: number;
  longitude?: number;
}

export interface CommentDTO {
  id: number;
  postId: number;
  userId: number;
  content: string;
  createdAt: string; // ISO
  userName?: string;
}

export interface PostDTO {
  id: number;
  description: string;
  createdAt: string; // ISO string iz backa
  userId: number;
  image?: ImageDTO;
  location?: LocationDTO;
  likeCount: number;
  comments?: CommentDTO[];
  eligibleForAd: boolean;
}

export interface TopLikerDTO {
  userId: number;
  username: string;
  likeCount: number;
}

export interface TrendsSummaryDTO {
  totalPosts: number;
  postsLast30Days: number;
  top5Last7Days: PostDTO[];
  top10AllTime: PostDTO[];
  topLikers7Days: TopLikerDTO[];
}
