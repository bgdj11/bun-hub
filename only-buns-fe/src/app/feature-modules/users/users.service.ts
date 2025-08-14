import { Injectable } from '@angular/core';
import { ApiService } from '../../infrastructure/api.service';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { User } from '../../infrastructure/auth/model/user';
import { HttpParams } from '@angular/common/http';
import {Follow} from '../../shared/model/follow';
import {Post} from '../posts/model/post';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly whoamiUrl = 'http://localhost:8080/api/whoami'; // URL za trenutnog korisnika
  private readonly usersUrl = 'http://localhost:8080/api/user/all'; // URL za listu svih korisnika (samo za admin)
  private readonly apiBase = 'http://localhost:8080/api';
  currentUser!: any;

  constructor(private apiService: ApiService) {}

  // Metoda za preuzimanje informacija o trenutno prijavljenom korisniku
  getMyInfo(): Observable<any> {
    return this.apiService.get(this.whoamiUrl).pipe(
      map((user) => {
        this.currentUser = user; // Čuva trenutnog korisnika u `currentUser`
        return user;
      })
    );
  }

  // Metoda za preuzimanje liste svih korisnika (samo za admina)
  getAll(): Observable<any> {
    return this.apiService.get(this.usersUrl);
  }

  getUsers(params: {
    sortBy: string;
    isAscending: boolean;
    name?: string;
    email?: string;
    minPosts?: number;
    maxPosts?: number;
  }): Observable<User[]> {

    // Kreiramo URL string sa svim parametrima, uključujući i prazne vrednosti
    const url = `http://localhost:8080/api/user/sort?sortBy=${params.sortBy}&isAscending=${params.isAscending}` +
      `&name=${params.name || ''}&email=${params.email || ''}` +
      `&minPosts=${params.minPosts !== undefined ? params.minPosts : ''}` +
      `&maxPosts=${params.maxPosts !== undefined ? params.maxPosts : ''}`;

    console.log("Sending direct URL with parameters:", url);
    return this.apiService.get(url);
  }

  changePassword(payload: {
    currentPassword: string;
    newPassword: string;
    confirmNewPassword: string;
  }): Observable<any> {
    return this.apiService.put(`${this.apiBase}/user/password`, payload);
  }


  // Preuzimanje korisnika prema ID
  getUserById(userId: number): Observable<any> {
    return this.apiService.get(`http://localhost:8080/api/user/${userId}`);
  }

  // Ažuriranje adrese korisnika
  updateAddress(userId: number, address: string): Observable<any> {
    return this.apiService.put(`http://localhost:8080/api/user/${userId}/update-address`, { address });
  }

// Praćenje korisnika
  followUser(userId: number): Observable<any> {
    return this.apiService.post(`${this.apiBase}/follow/${userId}`, {});
  }

  // Prekid praćenja korisnika
  unfollowUser(userId: number): Observable<any> {
    return this.apiService.delete(`${this.apiBase}/follow/${userId}`);
  }

  // Preuzimanje pratilaca
  getFollowers(userId: number): Observable<Follow[]> {
    return this.apiService.get(`${this.apiBase}/follow/followers/${userId}`);
  }

  // Preuzimanje korisnika koje korisnik prati
  getFollowing(userId: number): Observable<Follow[]> {
    return this.apiService.get(`${this.apiBase}/follow/following/${userId}`);
  }

  getUserByEmail(email: string): Observable<User> {
    const url = `http://localhost:8080/api/user/searchEmail?email=${encodeURIComponent(email)}`;
    return this.apiService.get(url);
  }

  getUserPosts(userId: number): Observable<Post[]> {
    return this.apiService.get(`http://localhost:8080/api/posts/user/${userId}`);
  }

  getCachedImage(imagePath: string): Observable<string> {
    const cacheKey = `cached_${imagePath}`;
    const cachedUrl = localStorage.getItem(cacheKey);

    if (cachedUrl) {
      return new Observable((observer) => {
        observer.next(cachedUrl);
        observer.complete();
      });
    } else {
      const fullUrl = `http://localhost:8080/uploads/${imagePath}`;
      return new Observable((observer) => {
        // Simulacija keširanja (može se povezati sa pravim API-jem za obradu slika)
        setTimeout(() => {
          localStorage.setItem(cacheKey, fullUrl);
          observer.next(fullUrl);
          observer.complete();
        }, 100); // Simuliraj kratko kašnjenje za "keširanje"
      });
    }
  }
}
