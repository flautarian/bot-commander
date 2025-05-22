export interface TaskDto {
    id: string,
    actionType: string,
    parameters: Map<string, string>
    result: string,
}