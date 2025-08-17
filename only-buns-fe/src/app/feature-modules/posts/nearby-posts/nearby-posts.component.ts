import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import * as L from 'leaflet';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AuthService } from '../../../infrastructure/auth/auth.service';
import { PostService } from '../post.service';
import { Like } from '../model/like';

interface PostDTO {
  id: number;
  description: string;
  createdAt: string;
  userId: number;
  image?: { path?: string; url?: string };
  location?: {
    country: string;
    city: string;
    address: string;
    number: number;
    latitude: number;
    longitude: number;
  };
  likeCount: number;
  comments: Array<{
    id?: number;
    userId?: number;
    userName?: string;
    content: string;
    createdAt: string;
  }>;
  eligibleForAd: boolean;
}

@Component({
  selector: 'app-nearby-posts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe],
  templateUrl: './nearby-posts.component.html',
  styleUrls: ['./nearby-posts.component.css'],
})
export class NearbyPostsComponent implements OnInit, OnDestroy {
  private map!: L.Map;
  private postsLayer = L.layerGroup();
  private userMarker: L.CircleMarker | null = null;
  private radiusCircle: L.Circle | null = null;

  radiusKm = 5;
  loading = false;

  posts: PostDTO[] = [];
  selectedPost: PostDTO | null = null;
  selectedComment = '';

  // like status keš za brzu UI reakciju
  private userLikes: Like[] = [];
  private currentUserId = 0;

  private apiBase = 'http://localhost:8080/api';

  // fallback: Beograd
  private centerLat = 44.8176;
  private centerLng = 20.4633;

  constructor(
    private http: HttpClient,
    private postService: PostService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.currentUserId = this.authService.getUserId();
      // Učitaj šta je korisnik lajkovao (da znamo disable stanja)
      this.postService.getLikes().subscribe({
        next: (likes) => (this.userLikes = likes || []),
        complete: () => {
          this.initMap();
          this.centerOnProfileThenLoad();
        },
        error: () => {
          this.initMap();
          this.centerOnProfileThenLoad();
        },
      });
    } else {
      this.initMap();
      this.centerOnProfileThenLoad();
    }
  }

  ngOnDestroy(): void {
    if (this.map) this.map.remove();
  }

  // ---------------- MAPA ----------------
  private initMap() {
    this.map = L.map('nearbyMap', {
      center: L.latLng(this.centerLat, this.centerLng),
      zoom: 13,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.postsLayer.addTo(this.map);
    this.map.on('moveend', () => this.loadInBounds());
  }

  private centerOnProfileThenLoad() {
    this.http.get<any>(`${this.apiBase}/whoami`).subscribe({
      next: (me) => {
        const loc = me?.location;
        if (loc?.latitude && loc?.longitude) {
          this.centerLat = loc.latitude;
          this.centerLng = loc.longitude;
          this.map.setView([this.centerLat, this.centerLng], 14);
        }
        this.drawUserMarkerAndRadius();
      },
      complete: () => this.loadNearby(),
      error: () => {
        this.drawUserMarkerAndRadius();
        this.loadInBounds();
      },
    });
  }

  private drawUserMarkerAndRadius() {
    if (this.userMarker) this.userMarker.remove();
    this.userMarker = L.circleMarker([this.centerLat, this.centerLng], {
      radius: 7,
      color: '#1f7a1f',
      weight: 2,
      fillColor: '#32cd32',
      fillOpacity: 0.8,
    })
      .bindPopup('<b>You are here</b>')
      .addTo(this.map);

    const radiusMeters = this.radiusKm * 1000;
    if (this.radiusCircle) this.radiusCircle.remove();
    this.radiusCircle = L.circle([this.centerLat, this.centerLng], {
      radius: radiusMeters,
      color: '#3388ff',
      weight: 1.5,
      fillOpacity: 0.05,
    }).addTo(this.map);
  }

  // ---------------- API ----------------
  loadNearby() {
    this.loading = true;
    this.http
      .get<PostDTO[]>(
        `${this.apiBase}/posts/nearby?centerLat=${this.centerLat}&centerLng=${this.centerLng}&radiusKm=${this.radiusKm}`
      )
      .subscribe({
        next: (posts) => {
          this.posts = posts;
          this.renderPosts(posts);
        },
        error: () => this.loadInBounds(),
        complete: () => (this.loading = false),
      });
  }

  loadInBounds() {
    const b = this.map.getBounds();
    const minLat = b.getSouth();
    const maxLat = b.getNorth();
    const minLng = b.getWest();
    const maxLng = b.getEast();

    this.loading = true;
    this.http
      .get<PostDTO[]>(
        `${this.apiBase}/posts/in-bounds?minLat=${minLat}&maxLat=${maxLat}&minLng=${minLng}&maxLng=${maxLng}`
      )
      .subscribe({
        next: (posts) => {
          this.posts = posts;
          this.renderPosts(posts);
        },
        error: () => {},
        complete: () => (this.loading = false),
      });
  }

  onRadiusChange() {
    this.drawUserMarkerAndRadius();
    this.loadNearby();
  }

  recenter() {
    this.map.setView([this.centerLat, this.centerLng], 14);
    this.drawUserMarkerAndRadius();
    this.loadNearby();
  }

  // ---------------- RENDER & INTERAKCIJE ----------------
  private renderPosts(posts: PostDTO[]) {
    this.postsLayer.clearLayers();

    posts.forEach((p) => {
      const ll = p.location;
      if (!ll) return;

      const m = L.circleMarker([ll.latitude, ll.longitude], {
        radius: 6,
        color: '#2a6ff5',
        weight: 2,
        fillColor: '#2a6ff5',
        fillOpacity: 0.85,
      });

      const addr = [
        ll.address && (ll.address + (ll.number ? ' ' + ll.number : '')),
        ll.city,
        ll.country,
      ]
        .filter(Boolean)
        .join(', ');

      const popupImg = p.image?.path
        ? `<div style="width:260px;height:180px;overflow:hidden;border-radius:8px;margin:.5rem 0;">
             <img src="http://localhost:8080/uploads/${encodeURI(
          p.image.path
        )}" style="width:100%;height:100%;object-fit:cover;display:block;" />
           </div>`
        : '';

      const popupHtml = `
        <div style="min-width:260px;max-width:300px">
          <strong>Post #${p.id}</strong><br/>
          <small>${new Date(p.createdAt).toLocaleString()}</small><br/>
          <em>${addr}</em>
          ${popupImg}
          <p style="margin:.25rem 0 0; white-space:pre-wrap">${this.escapeHtml(
        p.description ?? ''
      )}</p>
          <small>Likes: ${p.likeCount}</small>
        </div>
      `;

      m.bindPopup(popupHtml);
      m.on('click', () => this.onPostSelected(p));
      m.addTo(this.postsLayer);
    });
  }

  onPostSelected(post: PostDTO) {
    this.selectedPost = {
      ...post,
      // osveži sliku u “card” prikazu
      image: post.image?.path
        ? { ...post.image, url: `http://localhost:8080/uploads/${encodeURI(post.image.path)}` }
        : post.image,
    };
    this.selectedComment = '';
    setTimeout(() => {
      const el = document.getElementById('selectedPostBlock');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  }

  // ------- LIKE / COMMENT za selektovani post -------
  get selectedIsLiked(): boolean {
    if (!this.selectedPost || !this.authService.isAuthenticated()) return false;
    return this.userLikes.some(
      (l) => l.postId === this.selectedPost!.id && l.userId === this.currentUserId
    );
  }

  likeSelected(): void {
    if (!this.selectedPost || this.selectedIsLiked) return;
    this.postService.likePost(this.selectedPost.id).subscribe({
      next: (updated) => {
        // uvećaj broj i keširaj lajk u memoriji
        this.selectedPost!.likeCount = updated.likeCount ?? (this.selectedPost!.likeCount + 1);
        this.userLikes.push({ id: 0, postId: this.selectedPost!.id, userId: this.currentUserId } as Like);
      },
    });
  }

  private toViewComment(c: any) {
    return {
      id: c.id,
      userId: c.userId,
      userName: c.userName,
      content: c.content,
      createdAt: typeof c.createdAt === 'string'
        ? c.createdAt
        : new Date(c.createdAt).toISOString(), // <-- prebacimo u string
    };
  }

  addCommentSelected(): void {
    if (!this.selectedPost || !this.authService.isAuthenticated()) return;
    const content = (this.selectedComment || '').trim();
    if (!content) return;

    this.postService.addComment(this.selectedPost.id, this.currentUserId, content).subscribe({
      next: (toAdd) => {
        const view = this.toViewComment(toAdd);

        this.selectedPost!.comments = [view, ...(this.selectedPost!.comments || [])];
      },
    });
  }

  imgUrl(p?: PostDTO): string | null {
    if (!p?.image?.path && !p?.image?.url) return null;
    return p.image!.url || `http://localhost:8080/uploads/${encodeURI(p.image!.path!)}`;
  }

  // XSS safety
  private escapeHtml(s: string) {
    return s.replace(/[&<>"'`=\/]/g, (c) =>
      ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
        '/': '&#x2F;',
        '`': '&#x60;',
        '=': '&#x3D;',
      } as any)[c]
    );
  }
}
