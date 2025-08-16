import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { UserService } from '../../../feature-modules/users/users.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// HTTP
import { HttpClient } from '@angular/common/http';

// Leaflet (namespace)
import * as L from 'leaflet';

interface DisplayMessage {
  msgType: string;
  msgBody: string;
}

@Component({
  selector: 'app-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.css'],
})
export class SignUpComponent implements OnInit, OnDestroy {
  title = 'Sign up';
  form!: FormGroup;
  submitted = false;
  notification!: DisplayMessage;
  returnUrl!: string;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  // Leaflet state
  options!: L.MapOptions;
  map: L.Map | null = null;
  mapMarker: L.Marker | null = null;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private formBuilder: FormBuilder,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.params
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe((params: any) => {
        this.notification = params as DisplayMessage;
      });

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';

    // Form sa ugnježdenim "location"
    this.form = this.formBuilder.group(
      {
        username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(64)]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(32)]],
        confirmPassword: ['', Validators.required],
        firstname: ['', Validators.required],
        lastname: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]],
        location: this.formBuilder.group({
          country: [''],
          city: [''],
          address: [''],
          number: [0],
          latitude: [44.8176, Validators.required],   // Beograd
          longitude: [20.4633, Validators.required],
        }),
      },
      { validators: this.passwordMatchValidator }
    );

    // Leaflet opcije
    const lat = Number(this.form.get('location.latitude')?.value ?? 44.8176);
    const lng = Number(this.form.get('location.longitude')?.value ?? 20.4633);

    this.options = {
      layers: [
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 18,
          attribution: 'Map data © OpenStreetMap contributors',
        }),
      ],
      zoom: 13,
      center: L.latLng(lat, lng),
    };
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  // Validacija poklapanja lozinki
  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { mismatch: true };
  }

  // MAPA
  onMapReady(map: L.Map) {
    this.map = map;

    const lat = Number(this.form.get('location.latitude')?.value ?? 44.8176);
    const lng = Number(this.form.get('location.longitude')?.value ?? 20.4633);
    this.addMarker(lat, lng);

    map.on('click', (e: any) => {
      const newLat = e?.latlng?.lat;
      const newLng = e?.latlng?.lng;
      if (typeof newLat !== 'number' || typeof newLng !== 'number') return;

      this.form.patchValue({ location: { latitude: newLat, longitude: newLng } });

      if (this.mapMarker) this.map!.removeLayer(this.mapMarker);
      this.addMarker(newLat, newLng);

      this.reverseGeocode(newLat, newLng);
    });
  }

  addMarker(lat: number, lng: number) {
    this.mapMarker = L.marker([lat, lng], {
      icon: new L.Icon({
        iconUrl: 'assets/marker-icon.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
      }),
    }).addTo(this.map!);
  }

  // Reakcija na promenu adresnih polja → geokodiranje
  onAddressChange() {
    this.geocodeAddress();
  }

  geocodeAddress() {
    const loc = this.form.get('location')!.value;
    const addressStr = `${loc.address ?? ''} ${loc.number ?? ''}, ${loc.city ?? ''}, ${loc.country ?? ''}`.trim();
    if (!addressStr) return;

    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(addressStr)}&format=json&limit=1`;

    this.http.get(url).subscribe({
      next: (data: any) => {
        if (Array.isArray(data) && data.length > 0) {
          const res = data[0];
          const lat = parseFloat(res.lat);
          const lon = parseFloat(res.lon);

          this.form.patchValue({ location: { latitude: lat, longitude: lon } });

          if (this.map) {
            this.map.setView([lat, lon], 13);
            if (this.mapMarker) this.map.removeLayer(this.mapMarker);
            this.addMarker(lat, lon);
          }
        }
      },
      error: (err) => console.error('Greška pri geokodiranju adrese:', err),
    });
  }

  reverseGeocode(lat: number, lng: number) {
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`;

    this.http.get(url).subscribe({
      next: (data: any) => {
        if (data && data.address) {
          const country = data.address.country || '';
          const city = data.address.city || data.address.town || data.address.village || '';
          const address = data.address.road || '';
          const numParsed = data.address.house_number ? parseInt(data.address.house_number, 10) : 0;

          this.form.patchValue({
            location: {
              country,
              city,
              address,
              number: Number.isFinite(numParsed) ? numParsed : 0,
            },
          });
        }
      },
      error: (err) => console.error('Greška pri reverznom geokodiranju:', err),
    });
  }

  // Submit
  onSubmit() {
    this.notification;
    this.submitted = true;

    if (this.form.invalid) {
      this.submitted = false;
      return;
    }

    const payload = {
      username: this.form.value.username,
      password: this.form.value.password,
      firstname: this.form.value.firstname,
      lastname: this.form.value.lastname,
      email: this.form.value.email,
      location: this.form.value.location,
    };

    this.authService.signup(payload).subscribe(
      () => {
        this.authService
          .login({ username: payload.username, password: payload.password })
          .subscribe(() => {
            this.userService.getMyInfo().subscribe();
          });

        this.router.navigate([this.returnUrl]);
      },
      (error) => {
        this.submitted = false;
        this.notification = {
          msgType: 'error',
          msgBody: error['error']?.message || 'Registration failed.',
        };
      }
    );
  }
}
