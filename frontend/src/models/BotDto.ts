import { TaskDto } from "./TaskDto";

export interface BotDto {
    id: string,
    name: string,
    os: string,
    geolocation: string,
    payloadType: string,
    lastSignal: number,
    tasks: TaskDto[],
}