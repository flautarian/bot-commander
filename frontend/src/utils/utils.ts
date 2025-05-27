import { ActionDto } from "models/ActionDto";
import { ActionTypeDto } from "models/ActionTypeDto";
import { BotDto } from "models/BotDto";
import { HeartBeatEventDto } from "models/HeartBeatEventDto";
import { TaskDto } from "models/TaskDto";
import { UpdateTaskEventDto } from "models/UpdateTaskEventDto";

export class Utils {
    static getActionTypes(): ActionTypeDto[] {
        return [{
            name: 'utils.actionTypes.exec_script',
            description: 'utils.actionTypes.exec_script_desc',
            value: 'exec_script',
            parameters: new Map<string, any>([["value", ""]])
        }];
    }

    static instanceOfBotDto(object: any): object is BotDto {
        return 'id' in object && 'name' in object && 'lastSignal' in object && 'tasks' in object;
    }

    static instanceOfActionTypeDto(object: any): object is ActionTypeDto {
        return 'name' in object && 'description' in object && 'value' in object;
    }

    static instanceOfTaskDto(object: any): object is TaskDto {
        return 'id' in object && 'actionType' in object && 'parameters' in object && 'result' in object;
    }

    static instanceOfActionDto(object: any): object is ActionDto {
        return 'selectedBots' in object && 'actionType' in object && 'parameters' in object;
    }

    static instanceOfUpdateTaskEventDto(object: any): object is UpdateTaskEventDto {
        return 'botId' in object && 'taskId' in object && 'result' in object;
    }

    static instanceOfHeartBeatEventDto(object: any): object is HeartBeatEventDto {
        return 'botId' in object && !('taskId' in object);
    }
}
