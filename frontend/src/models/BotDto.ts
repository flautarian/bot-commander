import { TaskDto } from "./TaskDto";

export interface BotDto {
    id: string,
    name: string,
    os: string,
    geolocation: string,
    lastSignal: number,
    tasks: TaskDto[],
}