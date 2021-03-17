import { StringLiteralType } from "typescript";

// Dashboard Tab Info interface
export interface DashboardTabIF {
  tabName: string
  tabOrder: number
  tabRoles: string[]
  tiles: DashboardTileIF[]
}

export interface DashboardTileIF {
  tileName: string
  tileOrder: number
  tileType: string
  tileURL: string
  tileRoles: string[]
}
