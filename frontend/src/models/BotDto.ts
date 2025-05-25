import { TaskDto } from "./TaskDto";

export interface BotDto {
    id: string,
    name: string,
    os: string,
    status: string,
    tasks: TaskDto[],
}