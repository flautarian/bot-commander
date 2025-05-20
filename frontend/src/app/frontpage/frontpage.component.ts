import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActionDto } from 'models/ActionDto';

import { Utils } from 'utils/utils'; // Import the utility class
import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { environment } from 'environments/environment';
import { BotWebSocketService } from '@services/BotWebSocketService';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-frontpage',
    templateUrl: './frontpage.component.html',
    styleUrls: ['./frontpage.component.css']
})
export class FrontpageComponent implements OnInit {

    form!: FormGroup;

    bots: any;

    actionTypes = Utils.getActionTypes();

    isModalOpen = false;

    private currencyPipe: CurrencyPipe = new CurrencyPipe('en-EN');

    formData: ActionDto = {
        selectedBots: [],
        taskType: '',
        parameters: [],
    };

    constructor(
        private fb: FormBuilder,
        private http: HttpClient,
        private botWebSocketService: BotWebSocketService,
        private toastr: ToastrService) {
    }

    ngOnInit(): void {
        this.form = this.fb.group({
            selectedBots: [this.formData.selectedBots, Validators.minLength(1)],
            taskType: [this.formData.taskType, Validators.required],
            taskValue: [this.formData.parameters, Validators.required],
        });

        this.botWebSocketService.getUpdates().subscribe(update => {
            if (Array.isArray(update)) {
                this.bots = update;
            }
            else {
                let index = this.bots.findIndex((bot: any) => bot.botId === update.botId);
                if (index > -1)
                    this.bots[index].results?.push(update.result);
                else
                    this.bots.push({
                        botId: update.botId,
                        results: [update.result]
                    });
            }
        });
    }

    submitForm = () => {
        if (this.form.valid) {
            this.postRequestData({ ...this.form.value });
        } else
            this.toastr.error("Error detected in fields validation, please check the form and try again");
    }

    addValue = () => {
        this.formData?.parameters.push("");
    }

    removeValue = (index: number) => {
        this.formData?.parameters.splice(index, 1);
    }

    selectBot = (botId: string) => {
        if (this.formData?.selectedBots.includes(botId)) {
            this.formData.selectedBots = this.formData?.selectedBots.filter((bot) => bot !== botId);
        }
        else {
            this.formData?.selectedBots.push(botId);
        }
    }

    openModal = () => {
        this.isModalOpen = true;
        this.formData = {
            selectedBots: [],
            taskType: '',
            parameters: [],
        };    
    }

    closeModal = () => {
        this.isModalOpen = false;
    }

    /* 
      Post request of data
    */
    postRequestData = (data: ActionDto) => {
        this.http.post<any>(`${environment.apiUrl}/api/v1/process`, data)
            .subscribe(
                data => {
                    this.toastr.success('Action sent', "Action sent to bot " + data.selectedBots + " successfully");
                    //window.location.href = '/frontpage';
                },
                error => {
                    this.toastr.error('An error occurred', "Error occurred while sending an order to bot " + data.selectedBots + ", check console for more information");
                    console.error('Error:', error);
                    // Handle errors here
                }
            );
    }
}