import { Injectable } from '@angular/core';
import { webSocket } from 'rxjs/webSocket';
import { Observable } from 'rxjs';
import { environment } from 'environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  private socket$ = webSocket(environment.wsUrl);

  constructor() {}

  getUpdates(): Observable<any> {
    return this.socket$;
  }
}
