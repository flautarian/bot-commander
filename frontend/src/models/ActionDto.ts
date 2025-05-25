export interface ActionDto {
  selectedBots: string[],
  actionType: string,
  parameters: Map<string, any>,
}