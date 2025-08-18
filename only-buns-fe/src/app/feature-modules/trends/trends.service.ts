import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { TrendsSummaryDTO, PostDTO, TopLikerDTO } from './trends.models';

@Injectable({ providedIn: 'root' })
export class TrendsService {
  private baseUrl = 'http://localhost:8080/api/trends';

  constructor(private http: HttpClient) {}

  /** Uvek svež (bez internog TTL keša) */
  getSummary(): Observable<TrendsSummaryDTO> {
    return this.http.get<TrendsSummaryDTO>(`${this.baseUrl}/summary`);
  }

  /** FRESH varijanta — dodatno razbija browser/SW keš */
  getSummaryFresh(): Observable<TrendsSummaryDTO> {
    const headers = new HttpHeaders({
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache',
    });
    const params = new HttpParams().set('_', Date.now().toString()); // cache-buster
    return this.http
      .get<TrendsSummaryDTO>(`${this.baseUrl}/summary`, { headers, params })
      .pipe(shareReplay(1)); // samo da koegzistira sa paralelnim pozivima
  }

  /** Ako želiš, i ovo držiš bez keša da sve bude sveže */
  getTopPostsLast7Days(): Observable<PostDTO[]> {
    return this.http.get<PostDTO[]>(`${this.baseUrl}/top-posts/last7days`);
  }

  getTopPostsAllTime(): Observable<PostDTO[]> {
    return this.http.get<PostDTO[]>(`${this.baseUrl}/top-posts/alltime`);
  }

  getTopLikers7Days(): Observable<TopLikerDTO[]> {
    return this.http.get<TopLikerDTO[]>(`${this.baseUrl}/top-likers/last7days`);
  }

  /** Više nam ne treba internI TTL keš */
  invalidate() {
    // noop – ostavljeno za pozive iz komponente, ali nema internog keša
  }
}
