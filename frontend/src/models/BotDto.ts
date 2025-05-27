import { TaskDto } from "./TaskDto";

export interface BotDto {
    id: string,
    name: string,
    os: string,
    lastSignal: number,
    tasks: TaskDto[],
}