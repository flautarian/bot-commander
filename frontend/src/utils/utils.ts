import { ActionTypeDto } from "models/ActionTypeDto";

export class Utils {
    static getActionTypes(): ActionTypeDto[] {
        return [{
            name : 'utils.actionTypes.exec_script',
            description: 'utils.actionTypes.exec_script_desc',
            value: 'exec_script',
        }];
    }
}
