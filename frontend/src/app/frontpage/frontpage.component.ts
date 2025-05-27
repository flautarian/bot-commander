import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActionDto } from 'models/ActionDto';

import { Utils } from 'utils/utils'; // Import the utility class
import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { environment } from 'environments/environment';
import { BotWebSocketService } from '@services/BotWebSocketService';
import { ToastrService } from 'ngx-toastr';
import { BotDto } from 'models/BotDto';
import { UpdateTaskEventDto } from 'models/UpdateTaskEventDto';
import { ActionTypeDto } from 'models/ActionTypeDto';
import { HeartBeatEventDto } from 'models/HeartBeatEventDto';

@Component({
    selector: 'app-frontpage',
    templateUrl: './frontpage.component.html',
    styleUrls: ['./frontpage.component.css']
})
export class FrontpageComponent implements OnInit {
    form!: FormGroup;
    payloadForm!: FormGroup;

    bots: BotDto[] = [];
    selectedBot: BotDto = {} as BotDto;
    paramEntries: Array<[string, any]> = [];

    actionTypes: ActionTypeDto[] = [];
    payloadTypes: string[] = ['python', 'javascript'];

    isModalOpen = false;
    isActionModalOpen = false;
    isPayloadGenModalOpen = false;
    allBotsSelected = false;
    @Output() actionSubmitted: EventEmitter<ActionDto> = new EventEmitter<ActionDto>();

    private currencyPipe: CurrencyPipe = new CurrencyPipe('en-US');

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
        private cdr: ChangeDetectorRef) {
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
                this.bots[index].tasks[taskIndex].result = uteDto.result;
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
        });
    }

    submitForm = () => {
        const parametersObj: Record<string, any> = {};
        this.formData.parameters.forEach((val: string, key: any) => {
            parametersObj[key] = val;
        });
        this.form.setValue({
            selectedBots: this.formData.selectedBots,
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
        /* this.paramEntries = this.getFormDataParameterEntries();
        this.cdr.detectChanges(); */
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


    closeModal = () => {
        this.isModalOpen = false;
    }

    closeActionsModal = () => {
        this.isActionModalOpen = false;
    }

    closePayloadGenModal = () => {
        this.isPayloadGenModalOpen = false;
    }

    cleanAndClosePayloadGenModal() {
        this.isPayloadGenModalOpen = false;
        this.payloadFormData = {
            payloadType: '',
            payloadUrl: '',
        };
        this.payloadForm.reset();
    }

    cleanAndCloseActionModal() {
        this.isActionModalOpen = false;
        this.formData = {
            selectedBots: [],
            actionType: '',
            parameters: new Map<string, any>(),
        };
        this.form.reset();
        this.paramEntries = this.getFormDataParameterEntries();
        this.cdr.detectChanges();
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
}