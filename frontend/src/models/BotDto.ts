import { TaskDto } from "./TaskDto";

export interface BotDto {
    id: string,
    name: string,
    status: string,
    tasks: TaskDto[],
}