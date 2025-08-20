import { Injectable } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ApiService } from '../api.service';
import { Router } from '@angular/router';
import { catchError, map, shareReplay } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly tokenKey = 'jwt';
  private readonly userIdKey = 'userId';
  private readonly loginUrl = 'http://localhost:8080/auth/login';
  private readonly signupUrl = 'http://localhost:8080/auth/signup';
  private userRole: string | null = null;
  private isAdminFlag: boolean = false;
  constructor(private apiService: ApiService, private router: Router) {}

  login(user: any) {
    const loginHeaders = new HttpHeaders({
      Accept: 'application/json',
      'Content-Type': 'application/json',
    });
    const body = {
      email: user.email,
      password: user.password,
    };
    return this.apiService
      .post(this.loginUrl, JSON.stringify(body), loginHeaders)
      .pipe(
        map((res) => {
          const token = res.body?.accessToken;
          const userId = res.body?.userId;
          console.log(userId + ' OVO JE USERID IZ BACKENDA');
          console.log(token);
          if (token && userId) {
            this.saveAuthData(token, userId);
            this.userRole = null;
            this.isAdminFlag = false;
          } else {
            console.error('Token not found in response:', res);
          }
        })
      );
  }

  signup(user: any) {
    const signupHeaders = new HttpHeaders({
      Accept: 'application/json',
      'Content-Type': 'application/json',
    });
    return this.apiService
      .post(this.signupUrl, JSON.stringify(user), signupHeaders)
      .pipe(
        map(() => {
          console.log('Sign up success');
        })
      );
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userIdKey);
    this.userRole = null; // Resetujemo ulogu
    this.isAdminFlag = false; // Resetujemo admin flag
    this.router.navigate(['/login']);
  }

  tokenIsPresent(): boolean {
    return localStorage.getItem(this.tokenKey) !== null;
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  fetchUserRole(userId: string): Promise<string | null> {
    return this.apiService
      .get(`http://localhost:8080/api/user/${userId}`)
      .pipe(
        map((user) => {
          if (!user || !user.role ) {
            console.log(
              'Uloga nije pronađena ili je prazna, postavljam na prazan string'
            );
            this.userRole = ''; // Prazan string ako nema uloge
          } else {
            this.userRole = user.role; // Sačuvaj pravu ulogu
          }
          console.log(this.userRole);
          return this.userRole;
        })
      )
      .toPromise() as Promise<string | null>; // Forsiramo tip Promise<string | null>
  }

  async checkIfAdmin(): Promise<boolean> {
    // if (this.userRole !== null) {
    //   return this.isAdminFlag; // Vraća keširanu vrednost
    // }

    const userId = this.getUserId();
    if (!userId) return false;

    const role = await this.fetchUserRole(userId.toString());
    this.isAdminFlag = role === 'ROLE_ADMIN'; // Postavlja konačnu vrednost na osnovu uloge
    console.log('USER JE ADMIN ' + this.isAdminFlag);
    return this.isAdminFlag;
  }

  isAdmin(): boolean {
    return this.isAdminFlag; // Može se koristiti direktno u komponentama nakon checkIfAdmin
  }

  getUserId(): number {
    const userId = localStorage.getItem(this.userIdKey);
    return userId ? Number(userId) : 0; // Konvertujemo u number, ili vraćamo 0 ako userId nije definisan
  }

  private saveAuthData(token: string, userId: string): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userIdKey, userId); // Čuvamo i userId
  }
}
