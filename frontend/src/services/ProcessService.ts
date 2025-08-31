import { Injectable } from '@angular/core';
import { webSocket } from 'rxjs/webSocket';
import { Observable } from 'rxjs';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ProcessService {

  findAll(): Observable<any> {
    return new Observable(observer => {
      fetch(`${environment.apiUrl}/processes`)
        .then(response => response.json())
        .then(data => {
          observer.next(data);
          observer.complete();
        })
        .catch(error => observer.error(error));
    });
  }
}
