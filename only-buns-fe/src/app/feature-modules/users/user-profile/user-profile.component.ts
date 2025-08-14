import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgForOf, NgIf } from '@angular/common';
import { RouterModule } from '@angular/router'; // <-- NOVO
import { UserService } from '../users.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Follow } from '../../../shared/model/follow';
import { ActivatedRoute, Router } from '@angular/router';
import { PostComponent } from '../../posts/post/post.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    FormsModule,
    NgForOf,
    NgIf,
    DatePipe,
    RouterModule,      // <-- NOVO: zbog [routerLink]
    PostComponent
  ],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit, OnDestroy {
  user: any = null;
  currentUser: any = null;
  isCurrentUserProfile = true;

  followers: Follow[] = [];
  following: Follow[] = [];

  emailToSearch = '';
  successMessage = '';
  errorMessage = '';
  followSuccess = '';
  posts: any[] = [];

  showFollowers = false; // toggle

  routeSubscription!: Subscription;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.params.subscribe((params) => {
      const userId = params['id'];
      if (userId) {
        this.isCurrentUserProfile = false;
        this.loadUser(+userId);
      } else {
        this.isCurrentUserProfile = true;
        this.loadCurrentUser();
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }

  // Broj pratilaca
  get followerCount(): number {
    return this.followers?.length ?? 0;
  }

  toggleFollowers(): void {
    this.showFollowers = !this.showFollowers;
  }

  loadCurrentUser(): void {
    this.userService.getMyInfo().subscribe(
      (user) => {
        this.user = user;
        this.currentUser = user;
        this.loadFollowers();   // bitno za broj + listu
        this.loadFollowing();
        this.loadUserPosts(this.currentUser.id);
      },
      () => (this.errorMessage = 'Failed to load user data.')
    );
  }

  loadUser(userId: number): void {
    this.userService.getUserById(userId).subscribe(
      (user) => {
        this.user = user;
        this.loadFollowers();        // bitno i za tuđ profil
        this.loadUserPosts(userId);
      },
      () => (this.errorMessage = 'Failed to load user profile.')
    );
  }

  loadFollowers(): void {
    if (!this.user?.id) return;
    this.userService.getFollowers(this.user.id).subscribe(
      (followers) => {
        // 204 No Content -> body je null
        this.followers = followers ?? [];
      },
      () => (this.followers = [])
    );
  }

  loadFollowing(): void {
    if (!this.user?.id) return;
    this.userService.getFollowing(this.user.id).subscribe(
      (following) => (this.following = following ?? []),
      () => (this.following = [])
    );
  }

  searchUserByEmail(): void {
    if (!this.emailToSearch.trim()) return;
    this.userService.getUserByEmail(this.emailToSearch).subscribe(
      (user) => this.router.navigate(['/profile', user.id]),
      () => (this.errorMessage = 'User not found.')
    );
    this.emailToSearch = '';
  }

  followUser(): void {
    if (!this.user) {
      this.errorMessage = 'No user to follow.';
      return;
    }
    this.userService.followUser(this.user.id).subscribe(
      () => {
        this.followSuccess = `You are now following ${this.user.username}!`;
        this.loadFollowers(); // odmah osveži broj
      },
      () => {}
    );
  }

  unfollowUser(userId: number): void {
    this.userService.unfollowUser(userId).subscribe(
      () => {
        this.loadFollowing();
        if (this.isCurrentUserProfile) this.loadFollowers();
      },
      () => {}
    );
  }

  loadUserPosts(userId: number): void {
    this.userService.getUserPosts(userId).subscribe(
      (posts) => {
        this.posts = posts.map((post: any) => {
          if (post.image?.path) {
            this.userService.getCachedImage(post.image.path).subscribe((cachedPath: string) => {
              post.image.url = cachedPath;
            });
          }
          return post;
        });
      },
      () => (this.errorMessage = 'Failed to load user posts.')
    );
  }
}
