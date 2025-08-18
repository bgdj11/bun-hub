import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Post } from '../posts/model/post';
import { Comment } from '../posts/model/comment';
import { ApiService } from '../../infrastructure/api.service';
import { Like } from '../posts/model/like';
import { HttpClient } from '@angular/common/http';
import {map, shareReplay} from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private postApiUrl = 'http://localhost:8080/api/posts';
  private likeApiUrl = 'http://localhost:8080/api/likes';
  private commentApiUrl = 'http://localhost:8080/api/comments';
  private readonly apiBase = 'http://localhost:8080';
  private imageCache = new Map<string, string>();

  constructor(private apiService: ApiService, private http: HttpClient) {}

  getAllPosts(): Observable<Post[]> {
    return this.apiService.get(this.postApiUrl);
  }

  likePost(postId: number): Observable<Post> {
    return this.apiService
      .postWithResponse<Post>(`${this.likeApiUrl}/${postId}`, null)
      .pipe(
        map((response) => {
          const post = response.body; // Ekstrakcija tela odgovora
          if (!post) {
            throw new Error('Invalid response: Post data not found');
          }
          return post; // Vraćamo ažurirani post objekat
        })
      );
  }

  addComment(
    postId: number,
    userId: number,
    content: string
  ): Observable<Comment> {
    return this.apiService.post(
      `${this.commentApiUrl}/${postId}?userId=${userId}`,
      JSON.stringify({ content })
    );
  }

  deletePost(postId: number, userId: number): Observable<void> {
    return this.apiService.delete(
      `${this.postApiUrl}/${postId}?userId=${userId}`
    );
  }

  updatePost(id: number, userId: number, newDescription: string) {
    return this.apiService.put(
      `${this.postApiUrl}/${id}?userId=${userId}`,
      newDescription
    );
  }

  getLikes(): Observable<Like[]> {
    return this.apiService.get(this.likeApiUrl);
  }

  createPost(post: Post, image: File): Observable<Post> {
    const formData = new FormData();

    // Dodaj JSON podatke kao Blob sa MIME tipom "application/json"
    formData.append(
      'post',
      new Blob([JSON.stringify(post)], { type: 'application/json' })
    );

    // Dodaj sliku
    formData.append('image', image);

    const createUrl = `${this.postApiUrl}/create`;

    return this.apiService.post(createUrl, formData);
  }

  getCachedImage(path: string): Observable<string> {
    const normalized = (path || '').replace(/^\/+/, '');
    const key = normalized;

    const cached = this.imageCache.get(key);
    if (cached) {
      return new Observable((obs) => {
        obs.next(cached);
        obs.complete();
      });
    }

    // Ako ti je “prava” ruta drugačija (npr. /api/images/{file}),
    // promeni URL ispod. Važno: ide preko HttpClient -> doda se JWT.
    const url = `${this.apiBase}/uploads/${encodeURIComponent(normalized)}`;

    return this.http.get(url, { responseType: 'blob' }).pipe(
      map((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        this.imageCache.set(key, objectUrl);
        return objectUrl;
      }),
      // shareReplay da se ne šalje više istih GET-ova paralelno
      shareReplay(1)
    );
  }

  /** Očisti sve blob URL-ove iz keša da ne curi memorija */
  clearImageCache() {
    for (const [, objectUrl] of this.imageCache) {
      URL.revokeObjectURL(objectUrl);
    }
    this.imageCache.clear();
  }

  getUserFeed(userId: number): Observable<Post[]> {
    console.log(`Fetching user feed for userId: ${userId}`);
    return this.apiService.get(`http://localhost:8080/api/posts/user-feed?userId=${userId}`);
  }

  updateAdEligibility(postId: number): Observable<Post> {
    return this.apiService
      .put(`${this.postApiUrl}/ad-eligibility/${postId}`, {})
      .pipe(
        map((response: any) => {
          // Pretpostavka da API vraća ažurirani post
          const updatedPost: Post = response;
          return updatedPost; // Osiguravamo da Observable vrati ažurirani post
        })
      );
  }
}
