import { HttpClient } from '@angular/common/http';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ActionDto } from 'models/ActionDto';

import { Utils } from 'utils/utils'; // Import the utility class
import { FormGroup, Validators, FormBuilder } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { environment } from 'environments/environment';
import { BotWebSocketService } from '@services/BotWebSocketService';
import { ToastrService } from 'ngx-toastr';
import { BotDto } from 'models/BotDto';
import { UpdateTaskEventDto } from 'models/UpdateTaskEventDto';

@Component({
    selector: 'app-frontpage',
    templateUrl: './frontpage.component.html',
    styleUrls: ['./frontpage.component.css']
})
export class FrontpageComponent implements OnInit {

    form!: FormGroup;

    bots: BotDto[] = [
        {
            id: "bot1",
            name: "Example Bot 1",
            status: "inactive",
            tasks: [
                {
                    id: "task1",
                    actionType: "restart",
                    parameters: new Map<string, string>([
                        ["reason", "maintenance"]
                    ]),
                    result: "pending"
                }
            ]
        },
        {
            id: "bot2",
            name: "Example Bot 2",
            status: "inactive",
            tasks: [
                {
                    id: "task2",
                    actionType: "restart",
                    parameters: new Map<string, string>([
                        ["reason", "maintenance"]
                    ]),
                    result: "pending"
                }
            ]
        }
    ];

    actionTypes = Utils.getActionTypes();

    isModalOpen = false;

    private currencyPipe: CurrencyPipe = new CurrencyPipe('en-EN');

    formData: ActionDto = {
        selectedBots: [],
        actionType: '',
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
            actionType: [this.formData.actionType, Validators.required],
            taskValue: [this.formData.parameters, Validators.required],
        });

        this.botWebSocketService.getUpdates().subscribe(update => {
            if (Array.isArray(update)) {
                this.bots = update;
            }
            else {
                // pass update to UpdateTaskEventDto
                // this is a single update
                let uteDto = update as UpdateTaskEventDto;
                // find bot index
                let index = this.bots.findIndex((bot: any) => bot.botId === uteDto.botId);
                //add result to task index
                let taskIndex = this.bots[index].tasks.findIndex((task: any) => task.taskId === uteDto.taskId);
                this.bots[index].tasks[taskIndex].result = uteDto.result;
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

    selectAllBots = () => {
        if (this.formData?.selectedBots.length === this.bots.length) {
            this.formData.selectedBots = [];
        }
        else {
            this.formData.selectedBots = this.bots.map((bot: any) => bot.botId);
        }
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
            actionType: '',
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