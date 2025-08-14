import { Component, OnInit, OnDestroy } from '@angular/core';
import { DatePipe, NgForOf, NgIf, NgClass } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Subscription } from 'rxjs';

import { UserService } from '../users.service';
import { AuthService } from '../../../infrastructure/auth/auth.service';
import { Follow } from '../../../shared/model/follow';
import { PostComponent } from '../../posts/post/post.component';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    // Template-driven (za postojeći [(ngModel)])
    FormsModule,
    // Reactive forms (za [formGroup], formControlName, (ngSubmit))
    ReactiveFormsModule,
    // Common directives
    NgForOf, NgIf, NgClass, DatePipe,
    // Router linkovi
    RouterModule,
    // Tvoj post list komponent
    PostComponent
  ],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit, OnDestroy {
  user: any = null;
  currentUser: any = null;
  isCurrentUserProfile = true;

  // Password change UI/logic
  passwordFormVisible = false;
  passwordForm!: FormGroup;
  submittingPassword = false;
  passwordNotification: { msgType: 'success' | 'error'; msgBody: string } | null = null;

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
    private router: Router,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    // Reaguj na /profile/:id ili /profile (me)
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

    // Reactive form za promenu lozinke
    this.passwordForm = this.fb.group(
      {
        currentPassword: ['', [Validators.required]],
        newPassword: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.maxLength(32),
            // opciono: bar jedno slovo i jedan broj; pooštri po potrebi
            Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)
          ]
        ],
        confirmNewPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator('newPassword', 'confirmNewPassword') }
    );
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }

  // --- UI helpers ---

  get followerCount(): number {
    return this.followers?.length ?? 0;
  }

  toggleFollowers(): void {
    this.showFollowers = !this.showFollowers;
  }

  togglePasswordForm(): void {
    this.passwordNotification = null;
    this.passwordFormVisible = !this.passwordFormVisible;
    if (this.passwordFormVisible) {
      this.passwordForm.reset();
    }
  }

  // --- Reactive forms validator ---

  private passwordMatchValidator(passwordKey: string, confirmKey: string) {
    return (group: AbstractControl) => {
      const password = group.get(passwordKey)?.value;
      const confirm = group.get(confirmKey)?.value;
      return password === confirm ? null : { mismatch: true };
    };
  }

  // --- Submit password change ---

  onSubmitPassword(): void {
    this.passwordNotification = null;

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const payload = this.passwordForm.value; // { currentPassword, newPassword, confirmNewPassword }
    this.submittingPassword = true;

    this.userService.changePassword(payload).subscribe({
      next: () => {
        this.submittingPassword = false;
        this.passwordNotification = { msgType: 'success', msgBody: 'Lozinka uspešno promenjena.' };
        this.passwordForm.reset();
        this.passwordFormVisible = false;
      },
      error: (err) => {
        this.submittingPassword = false;
        const msg = err?.error ?? 'Greška pri promeni lozinke.';
        this.passwordNotification = { msgType: 'error', msgBody: (typeof msg === 'string' ? msg : 'Greška pri promeni lozinke.') };
      }
    });
  }

  // --- Data loading ---

  loadCurrentUser(): void {
    this.userService.getMyInfo().subscribe(
      (user) => {
        this.user = user;
        this.currentUser = user;
        this.loadFollowers();   // broj + lista
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
        this.loadFollowers();        // i za tuđ profil
        this.loadUserPosts(userId);
      },
      () => (this.errorMessage = 'Failed to load user profile.')
    );
  }

  loadFollowers(): void {
    if (!this.user?.id) return;
    this.userService.getFollowers(this.user.id).subscribe(
      (followers) => {
        this.followers = followers ?? []; // 204 No Content -> null
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
