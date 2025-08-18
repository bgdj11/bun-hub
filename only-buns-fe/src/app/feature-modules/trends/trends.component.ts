import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { Observable, Subscription, filter } from 'rxjs';

import { TrendsService } from './trends.service';
import { TrendsSummaryDTO, PostDTO } from './trends.models';
import { AuthService } from '../../infrastructure/auth/auth.service';
import { PostService } from '../posts/post.service';

@Component({
  selector: 'app-trends',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, RouterModule, DatePipe],
  templateUrl: './trends.component.html',
  styleUrls: ['./trends.component.css'],
})
export class TrendsComponent implements OnInit, OnDestroy {
  summary$!: Observable<TrendsSummaryDTO>;
  loading = true;
  error: string | null = null;

  // postIds koje je TRENUTNI korisnik lajkovao
  liked = new Set<number>();
  commentDraft: Record<number, string> = {};

  /** postId -> blob: URL */
  private imageBlobCache = new Map<number, string>();
  private sub?: Subscription;
  private navSub?: Subscription;
  private visibilityHandler = () => {
    if (document.visibilityState === 'visible') {
      this.refresh(); // povuci potpuno svež summary kad se vrati fokus na tab
    }
  };

  constructor(
    private trendsService: TrendsService,
    private postService: PostService,
    public authService: AuthService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    // auto-refresh svaki put kad se navigira NA ovu rutu
    this.navSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        // Ako koristiš posebne rute za tabove i ova komponenta je aktivna,
        // u praksi je dovoljno uvek raditi fresh kad dođe NavigationEnd.
        this.refresh();
      });

    document.addEventListener('visibilitychange', this.visibilityHandler);

    if (this.authService.isAuthenticated()) {
      this.loadLikes();
    }
    // inicijalno učitavanje – odmah fresh
    this.load(true);
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.navSub?.unsubscribe();
    document.removeEventListener('visibilitychange', this.visibilityHandler);

    // Revoke lokalne objectURL-ove da ne curi memorija
    for (const [, url] of this.imageBlobCache) URL.revokeObjectURL(url);
    this.imageBlobCache.clear();

    // Ako nema clearImageCache u PostService, preskoči
    if ((this.postService as any).clearImageCache) {
      (this.postService as any).clearImageCache();
    }
  }

  /** Učitaj sve lajkove TRENUTNOG korisnika i popuni set */
  private loadLikes() {
    const uid = this.authService.getUserId();
    this.postService.getLikes().subscribe({
      next: (likes) => {
        this.liked.clear();
        for (const l of likes || []) {
          if (l?.userId === uid && typeof l.postId === 'number') {
            this.liked.add(l.postId);
          }
        }
      },
      error: (e) => console.error('loadLikes error', e),
    });
  }

  /** Učitavanje summary-ja; forceFresh=true zaobilazi SVE keševe */
  load(forceFresh = false) {
    this.loading = true;
    this.error = null;

    this.summary$ = forceFresh
      ? this.trendsService.getSummaryFresh()
      : this.trendsService.getSummary(); // u servisu je ionako bez keša

    this.sub?.unsubscribe();
    this.sub = this.summary$.subscribe({
      next: (summary) => {
        if (this.authService.isAuthenticated()) {
          (summary.top5Last7Days || []).forEach((p) => this.prefetchImage(p));
          (summary.top10AllTime || []).forEach((p) => this.prefetchImage(p));
        }
        this.loading = false;
      },
      error: (e) => {
        console.error(e);
        this.error = 'Greška pri učitavanju trendova.';
        this.loading = false;
      },
    });
  }

  /** Potpuni refresh: briše blob URL-ove i vuče FRESH summary */
  refresh() {
    // očisti blob URL-ove (slike će se ponovo prefetchnuti)
    for (const [, url] of this.imageBlobCache) URL.revokeObjectURL(url);
    this.imageBlobCache.clear();

    if ((this.postService as any).clearImageCache) {
      (this.postService as any).clearImageCache();
    }

    if (this.authService.isAuthenticated()) {
      this.loadLikes();
    }

    // povuci potpuno svež summary (sa cache-busterom)
    this.load(true);
  }

  /** Ako postoji image.path, povuci kao blob preko HttpClient (JWT ide automatski) i zapamti blob URL */
  private prefetchImage(p: PostDTO) {
    const path = p?.image?.path;
    if (!path || this.imageBlobCache.has(p.id)) return;

    this.postService.getCachedImage(path).subscribe({
      next: (url) => this.imageBlobCache.set(p.id, url),
      error: () => { /* ignoriši; imgUrl ima fallback */ },
    });
  }

  /**
   * Vraća:
   * - blob URL ako je u kešu (idealno),
   * - ako nema blob-a i korisnik NIJE ulogovan -> null (da ne teramo browser na 401 GET),
   * - ako nema blob-a i korisnik jeste ulogovan -> fallback na /uploads/{path} (dok blob ne stigne).
   */
  imgUrl(p?: PostDTO): string | null {
    if (!p?.image?.path) return null;

    const cached = this.imageBlobCache.get(p.id);
    if (cached) return cached;

    if (!this.authService.isAuthenticated()) return null;

    const path = p.image.path.trim();
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    const normalized = path.replace(/^\/+/, '');
    return `http://localhost:8080/uploads/${normalized}`;
  }

  /** koristi local set “liked” (popunjen iz getLikes i posle onLike) */
  isPostLiked(p: PostDTO) {
    return this.liked.has(p.id);
  }

  onLike(p: PostDTO) {
    if (!this.authService.isAuthenticated() || this.isPostLiked(p)) return;

    // optimistički update (UI odmah reaguje)
    this.liked.add(p.id);
    const oldCount = p.likeCount ?? 0;
    p.likeCount = oldCount + 1;

    this.postService.likePost(p.id).subscribe({
      next: (updatedPost) => {
        if (updatedPost?.likeCount != null) {
          p.likeCount = updatedPost.likeCount;
        }
        this.refresh(); // potpuno svež summary
      },
      error: (e) => {
        this.liked.delete(p.id);
        p.likeCount = oldCount;
        console.error('Like error', e);
      },
    });
  }

  onAddComment(p: PostDTO) {
    if (!this.authService.isAuthenticated()) return;

    const text = (this.commentDraft[p.id] || '').trim();
    if (!text) return;

    const userId = this.authService.getUserId();
    this.postService.addComment(p.id, userId, text).subscribe({
      next: () => {
        this.commentDraft[p.id] = '';
        this.refresh(); // potpuno svež summary
      },
      error: (e) => console.error('Comment error', e),
    });
  }
}
