import { NgModule } from '@angular/core';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { HomeComponent } from './feature-modules/layout/home/home.component';
import { AppRoutingModule } from './infrastructure/routing/app-routing.module';
import { AppComponent } from './app.component';
import { PostComponent } from './feature-modules/posts/post/post.component';
import { TokenInterceptor } from './infrastructure/interceptor/TokenInterceptor';
import { SignUpComponent } from './infrastructure/auth/sign-up/sign-up.component';
import { LoginComponent } from './infrastructure/auth/login/login.component';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { UserListComponent } from './feature-modules/users/user-list/user-list.component';
import { NearbyPostsComponent } from './feature-modules/posts/nearby-posts/nearby-posts.component';
import {TrendsComponent} from './feature-modules/trends/trends.component';
import {AnalyticsComponent} from './feature-modules/analytics/analytics.component';
import { UserProfileComponent} from './feature-modules/users/user-profile/user-profile.component';
import { LeafletModule } from '@asymmetrik/ngx-leaflet';

@NgModule({
    declarations: [
        AppComponent,
        HomeComponent,
        SignUpComponent,
        LoginComponent,
        UserListComponent,
    ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule, // Dodaj pravi modul za spinner
    BrowserAnimationsModule,
    FormsModule,
    NearbyPostsComponent,
    TrendsComponent,
    AnalyticsComponent,
    UserProfileComponent,
    PostComponent,
    LeafletModule,
  ],
    providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: TokenInterceptor,
            multi: true,
        },
    ],
    bootstrap: [AppComponent],
    exports: [
        PostComponent
    ]
})
export class AppModule {}
