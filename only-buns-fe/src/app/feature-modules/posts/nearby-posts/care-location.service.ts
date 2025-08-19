import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CareLocation } from '../model/care-location.model';

@Injectable({ providedIn: 'root' })
export class CareLocationService {
  private apiUrl = 'http://localhost:8080/api/care-locations';
  private orgAppUrl = 'http://localhost:8081/send';

  constructor(private http: HttpClient) {}

  triggerSendFromOrgApp(): Observable<string> {
    return this.http.post(this.orgAppUrl, {}, { responseType: 'text' });
  }

  getCareLocations(): Observable<CareLocation[]> {
    return this.http.get<CareLocation[]>(this.apiUrl);
  }
}
