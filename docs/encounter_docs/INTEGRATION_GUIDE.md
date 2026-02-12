# Frontend Integration Guide

## Overview
This guide explains how to integrate the Encounter module APIs into your Angular frontend application.

---

## Prerequisites

### 1. Authentication Service
Ensure you have an authentication service that:
- Stores JWT token
- Includes token in HTTP headers
- Handles token refresh
- Manages user context

### 2. HTTP Interceptor
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('jwt_token');
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

---

## Service Layer

### EncounterService

**File**: `encounter.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface EncounterCreateRequest {
  appointmentId: number;
  patientId: number;
  doctorId: number;
}

export interface EncounterUpdateRequest {
  chiefComplaint: string;
  diagnosis: string;
  notes: string;
}

export interface EncounterResponse {
  id: number;
  appointmentId: number;
  patientId: number;
  doctorId: number;
  status: 'TRIAGE' | 'IN_PROGRESS' | 'COMPLETED';
  chiefComplaint?: string;
  diagnosis?: string;
  notes?: string;
  startedAt: string;
  completedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EncounterService {
  private baseUrl = `${environment.apiUrl}/api/v1/encounters`;

  constructor(private http: HttpClient) {}

  startEncounter(request: EncounterCreateRequest): Observable<EncounterResponse> {
    return this.http.post<EncounterResponse>(this.baseUrl, request);
  }

  updateClinicalNotes(id: number, request: EncounterUpdateRequest): Observable<EncounterResponse> {
    return this.http.patch<EncounterResponse>(
      `${this.baseUrl}/${id}/clinical-notes`,
      request
    );
  }

  completeEncounter(id: number): Observable<EncounterResponse> {
    return this.http.patch<EncounterResponse>(
      `${this.baseUrl}/${id}/complete`,
      {}
    );
  }

  getEncounterById(id: number): Observable<EncounterResponse> {
    return this.http.get<EncounterResponse>(`${this.baseUrl}/${id}`);
  }

  getEncounterByAppointmentId(appointmentId: number): Observable<EncounterResponse> {
    return this.http.get<EncounterResponse>(
      `${this.baseUrl}/by-appointment/${appointmentId}`
    );
  }

  getTriageQueue(): Observable<EncounterResponse[]> {
    return this.http.get<EncounterResponse[]>(`${this.baseUrl}/queue/triage`);
  }

  getDoctorQueue(doctorId: number): Observable<EncounterResponse[]> {
    return this.http.get<EncounterResponse[]>(
      `${this.baseUrl}/queue/doctor/${doctorId}`
    );
  }
}
```

---

### VitalsService

**File**: `vitals.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface VitalsRequest {
  temperature?: number;
  systolic?: number;
  diastolic?: number;
  pulse?: number;
  spo2?: number;
  weight?: number;
  height?: number;
}

export interface VitalsResponse {
  id: number;
  encounterId: number;
  temperature?: number;
  systolic?: number;
  diastolic?: number;
  pulse?: number;
  spo2?: number;
  weight?: number;
  height?: number;
  bmi?: number;
  recordedAt: string;
  recordedBy: string;
}

@Injectable({
  providedIn: 'root'
})
export class VitalsService {
  private baseUrl = `${environment.apiUrl}/api/v1/encounters`;

  constructor(private http: HttpClient) {}

  saveVitals(encounterId: number, request: VitalsRequest): Observable<VitalsResponse> {
    return this.http.post<VitalsResponse>(
      `${this.baseUrl}/${encounterId}/vitals`,
      request
    );
  }

  getVitals(encounterId: number): Observable<VitalsResponse> {
    return this.http.get<VitalsResponse>(
      `${this.baseUrl}/${encounterId}/vitals`
    );
  }
}
```

---

### PrescriptionService

**File**: `prescription.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface PrescriptionItemRequest {
  medicineName: string;
  dosage?: string;
  frequency?: string;
  duration?: string;
}

export interface PrescriptionRequest {
  note?: string;
  items: PrescriptionItemRequest[];
}

export interface PrescriptionItemResponse {
  id: number;
  medicineName: string;
  dosage?: string;
  frequency?: string;
  duration?: string;
}

export interface PrescriptionResponse {
  id: number;
  encounterId: number;
  note?: string;
  status: 'DRAFT' | 'ISSUED';
  issuedAt?: string;
  items: PrescriptionItemResponse[];
}

@Injectable({
  providedIn: 'root'
})
export class PrescriptionService {
  private baseUrl = `${environment.apiUrl}/api/v1/encounters`;

  constructor(private http: HttpClient) {}

  savePrescription(encounterId: number, request: PrescriptionRequest): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(
      `${this.baseUrl}/${encounterId}/prescriptions`,
      request
    );
  }

  getPrescription(encounterId: number): Observable<PrescriptionResponse> {
    return this.http.get<PrescriptionResponse>(
      `${this.baseUrl}/${encounterId}/prescriptions`
    );
  }
}
```

---

## Component Examples

### 1. Triage Queue Component

**File**: `triage-queue.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { EncounterService, EncounterResponse } from '../services/encounter.service';

@Component({
  selector: 'app-triage-queue',
  templateUrl: './triage-queue.component.html'
})
export class TriageQueueComponent implements OnInit {
  encounters: EncounterResponse[] = [];
  loading = false;
  error: string | null = null;

  constructor(private encounterService: EncounterService) {}

  ngOnInit() {
    this.loadQueue();
  }

  loadQueue() {
    this.loading = true;
    this.encounterService.getTriageQueue().subscribe({
      next: (data) => {
        this.encounters = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load triage queue';
        this.loading = false;
        console.error(err);
      }
    });
  }

  refresh() {
    this.loadQueue();
  }
}
```

**Template**: `triage-queue.component.html`

```html
<div class="triage-queue">
  <h2>Triage Queue</h2>
  
  <button (click)="refresh()" [disabled]="loading">
    Refresh
  </button>

  <div *ngIf="loading">Loading...</div>
  <div *ngIf="error" class="error">{{ error }}</div>

  <table *ngIf="!loading && encounters.length > 0">
    <thead>
      <tr>
        <th>Patient ID</th>
        <th>Doctor ID</th>
        <th>Started At</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let encounter of encounters">
        <td>{{ encounter.patientId }}</td>
        <td>{{ encounter.doctorId }}</td>
        <td>{{ encounter.startedAt | date:'short' }}</td>
        <td>
          <button [routerLink]="['/vitals', encounter.id]">
            Record Vitals
          </button>
        </td>
      </tr>
    </tbody>
  </table>

  <div *ngIf="!loading && encounters.length === 0">
    No patients in triage queue
  </div>
</div>
```

---

### 2. Vitals Recording Component

**File**: `vitals-form.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { VitalsService } from '../services/vitals.service';

@Component({
  selector: 'app-vitals-form',
  templateUrl: './vitals-form.component.html'
})
export class VitalsFormComponent implements OnInit {
  vitalsForm: FormGroup;
  encounterId: number;
  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private vitalsService: VitalsService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.vitalsForm = this.fb.group({
      temperature: [null, [Validators.min(90), Validators.max(110)]],
      systolic: [null, [Validators.min(60), Validators.max(250)]],
      diastolic: [null, [Validators.min(40), Validators.max(150)]],
      pulse: [null, [Validators.min(40), Validators.max(200)]],
      spo2: [null, [Validators.min(70), Validators.max(100)]],
      weight: [null, [Validators.min(1), Validators.max(500)]],
      height: [null, [Validators.min(50), Validators.max(250)]]
    });
  }

  ngOnInit() {
    this.encounterId = +this.route.snapshot.params['id'];
    this.loadExistingVitals();
  }

  loadExistingVitals() {
    this.vitalsService.getVitals(this.encounterId).subscribe({
      next: (vitals) => {
        this.vitalsForm.patchValue(vitals);
      },
      error: (err) => {
        // Vitals don't exist yet, that's okay
        console.log('No existing vitals');
      }
    });
  }

  onSubmit() {
    if (this.vitalsForm.valid) {
      this.loading = true;
      this.vitalsService.saveVitals(this.encounterId, this.vitalsForm.value).subscribe({
        next: (response) => {
          this.loading = false;
          this.router.navigate(['/triage-queue']);
        },
        error: (err) => {
          this.error = 'Failed to save vitals';
          this.loading = false;
          console.error(err);
        }
      });
    }
  }
}
```

**Template**: `vitals-form.component.html`

```html
<div class="vitals-form">
  <h2>Record Vitals</h2>

  <form [formGroup]="vitalsForm" (ngSubmit)="onSubmit()">
    <div class="form-row">
      <label>Temperature (°F):</label>
      <input type="number" formControlName="temperature" step="0.1">
    </div>

    <div class="form-row">
      <label>Blood Pressure:</label>
      <input type="number" formControlName="systolic" placeholder="Systolic">
      /
      <input type="number" formControlName="diastolic" placeholder="Diastolic">
    </div>

    <div class="form-row">
      <label>Pulse (bpm):</label>
      <input type="number" formControlName="pulse">
    </div>

    <div class="form-row">
      <label>SpO2 (%):</label>
      <input type="number" formControlName="spo2">
    </div>

    <div class="form-row">
      <label>Weight (kg):</label>
      <input type="number" formControlName="weight" step="0.1">
    </div>

    <div class="form-row">
      <label>Height (cm):</label>
      <input type="number" formControlName="height" step="0.1">
    </div>

    <div *ngIf="error" class="error">{{ error }}</div>

    <button type="submit" [disabled]="!vitalsForm.valid || loading">
      {{ loading ? 'Saving...' : 'Save Vitals' }}
    </button>
  </form>
</div>
```

---

### 3. Doctor Consultation Component

**File**: `consultation.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EncounterService } from '../services/encounter.service';
import { VitalsService } from '../services/vitals.service';
import { PrescriptionService } from '../services/prescription.service';

@Component({
  selector: 'app-consultation',
  templateUrl: './consultation.component.html'
})
export class ConsultationComponent implements OnInit {
  encounterId: number;
  encounter: any;
  vitals: any;
  
  clinicalForm: FormGroup;
  prescriptionItems: any[] = [];
  
  loading = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private encounterService: EncounterService,
    private vitalsService: VitalsService,
    private prescriptionService: PrescriptionService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.clinicalForm = this.fb.group({
      chiefComplaint: ['', Validators.required],
      diagnosis: ['', Validators.required],
      notes: ['']
    });
  }

  ngOnInit() {
    this.encounterId = +this.route.snapshot.params['id'];
    this.loadEncounterData();
  }

  loadEncounterData() {
    this.loading = true;
    
    // Load encounter
    this.encounterService.getEncounterById(this.encounterId).subscribe({
      next: (encounter) => {
        this.encounter = encounter;
        this.clinicalForm.patchValue(encounter);
      },
      error: (err) => console.error(err)
    });

    // Load vitals
    this.vitalsService.getVitals(this.encounterId).subscribe({
      next: (vitals) => {
        this.vitals = vitals;
        this.loading = false;
      },
      error: (err) => {
        console.log('No vitals recorded');
        this.loading = false;
      }
    });

    // Load prescription
    this.prescriptionService.getPrescription(this.encounterId).subscribe({
      next: (prescription) => {
        this.prescriptionItems = prescription.items;
      },
      error: (err) => {
        console.log('No prescription yet');
      }
    });
  }

  saveClinicalNotes() {
    if (this.clinicalForm.valid) {
      this.encounterService.updateClinicalNotes(
        this.encounterId,
        this.clinicalForm.value
      ).subscribe({
        next: (response) => {
          this.encounter = response;
          alert('Clinical notes saved');
        },
        error: (err) => {
          this.error = 'Failed to save clinical notes';
          console.error(err);
        }
      });
    }
  }

  addPrescriptionItem() {
    this.prescriptionItems.push({
      medicineName: '',
      dosage: '',
      frequency: '',
      duration: ''
    });
  }

  removePrescriptionItem(index: number) {
    this.prescriptionItems.splice(index, 1);
  }

  savePrescription() {
    const request = {
      note: 'Take as prescribed',
      items: this.prescriptionItems
    };

    this.prescriptionService.savePrescription(this.encounterId, request).subscribe({
      next: (response) => {
        alert('Prescription saved');
      },
      error: (err) => {
        this.error = 'Failed to save prescription';
        console.error(err);
      }
    });
  }

  completeEncounter() {
    if (confirm('Are you sure you want to complete this encounter?')) {
      this.encounterService.completeEncounter(this.encounterId).subscribe({
        next: (response) => {
          alert('Encounter completed');
          this.router.navigate(['/doctor-queue']);
        },
        error: (err) => {
          this.error = 'Failed to complete encounter. Ensure diagnosis is provided.';
          console.error(err);
        }
      });
    }
  }
}
```

---

## Error Handling

### Global Error Interceptor

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Unauthorized - redirect to login
          this.router.navigate(['/login']);
        } else if (error.status === 403) {
          // Forbidden - show error message
          alert('You do not have permission to perform this action');
        } else if (error.status === 404) {
          // Not found
          alert('Resource not found');
        } else if (error.status === 409) {
          // Conflict - business rule violation
          alert(error.error.message || 'Operation not allowed');
        }
        
        return throwError(() => error);
      })
    );
  }
}
```

---

## Routing Configuration

```typescript
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { TriageQueueComponent } from './components/triage-queue.component';
import { VitalsFormComponent } from './components/vitals-form.component';
import { ConsultationComponent } from './components/consultation.component';

const routes: Routes = [
  {
    path: 'triage-queue',
    component: TriageQueueComponent,
    canActivate: [AuthGuard],
    data: { requiredPermission: 'CMP_VITALS_READ' }
  },
  {
    path: 'vitals/:id',
    component: VitalsFormComponent,
    canActivate: [AuthGuard],
    data: { requiredPermission: 'CMP_VITALS_WRITE' }
  },
  {
    path: 'consultation/:id',
    component: ConsultationComponent,
    canActivate: [AuthGuard],
    data: { requiredPermission: 'CMP_CONSULTATION_WRITE' }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
```

---

## State Management (NgRx - Optional)

### Actions

```typescript
import { createAction, props } from '@ngrx/store';
import { EncounterResponse } from '../services/encounter.service';

export const loadTriageQueue = createAction('[Encounter] Load Triage Queue');
export const loadTriageQueueSuccess = createAction(
  '[Encounter] Load Triage Queue Success',
  props<{ encounters: EncounterResponse[] }>()
);
export const loadTriageQueueFailure = createAction(
  '[Encounter] Load Triage Queue Failure',
  props<{ error: any }>()
);
```

### Effects

```typescript
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { EncounterService } from '../services/encounter.service';
import * as EncounterActions from './encounter.actions';

@Injectable()
export class EncounterEffects {
  loadTriageQueue$ = createEffect(() =>
    this.actions$.pipe(
      ofType(EncounterActions.loadTriageQueue),
      switchMap(() =>
        this.encounterService.getTriageQueue().pipe(
          map(encounters => EncounterActions.loadTriageQueueSuccess({ encounters })),
          catchError(error => of(EncounterActions.loadTriageQueueFailure({ error })))
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private encounterService: EncounterService
  ) {}
}
```

---

## Best Practices

### 1. Type Safety
Always use TypeScript interfaces for API requests/responses

### 2. Error Handling
- Use global error interceptor
- Show user-friendly error messages
- Log errors for debugging

### 3. Loading States
- Show loading indicators during API calls
- Disable buttons to prevent double-submission

### 4. Validation
- Client-side validation for better UX
- Server-side validation is still enforced

### 5. Caching (Optional)
```typescript
import { shareReplay } from 'rxjs/operators';

getEncounterById(id: number): Observable<EncounterResponse> {
  return this.http.get<EncounterResponse>(`${this.baseUrl}/${id}`)
    .pipe(shareReplay(1)); // Cache the result
}
```

---

## Testing

### Service Tests

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EncounterService } from './encounter.service';

describe('EncounterService', () => {
  let service: EncounterService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EncounterService]
    });
    service = TestBed.inject(EncounterService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should start encounter', () => {
    const mockRequest = { appointmentId: 1, patientId: 2, doctorId: 3 };
    const mockResponse = { id: 50, ...mockRequest, status: 'TRIAGE' };

    service.startEncounter(mockRequest).subscribe(response => {
      expect(response.id).toBe(50);
      expect(response.status).toBe('TRIAGE');
    });

    const req = httpMock.expectOne(`${service['baseUrl']}`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  afterEach(() => {
    httpMock.verify();
  });
});
```

---

## Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.hospital.com'
};
```
