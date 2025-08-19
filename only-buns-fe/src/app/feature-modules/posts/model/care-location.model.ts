export interface CareLocation {
  id: number;
  externalId: number;
  name: string;
  country?: string;
  city?: string;
  address?: string;
  number?: number;
  latitude: number;
  longitude: number;
}
