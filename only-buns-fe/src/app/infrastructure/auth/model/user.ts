import {GroupChatMember} from '../../../shared/model/groupChatMember';
import { Location } from '../../../shared/model/location';

export interface User {
  id: number;                     // ID korisnika
  email: string;                  // Email adresa
  username: string;               // Korisničko ime
  firstName: string;              // Ime
  lastName: string;               // Prezime
  location?: Location;
  isActive: boolean;              // Da li je korisnik aktivan (enabled)
  registeredAt: Date;             // Datum registracije (lastLoginDate)
  role: string;                   // Uloga korisnika
  postCount: number;              // Broj objava
  followingCount: number;         // Broj korisnika koje prati
  followerCount: number;          // Broj pratilaca
  sentMessageIds: number[];       // Lista ID-eva poslatih poruka
  groupIds: number[];             // Lista ID-eva grupa u kojima je korisnik član
  groupMemberships: GroupChatMember[]; // Lista članstva korisnika u grupama
}
