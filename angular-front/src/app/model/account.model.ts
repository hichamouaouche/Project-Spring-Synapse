export interface Account {
  id? : string,
  balance? : number,
  creationAt? : string,
  type? : string,
  customerId? : number

}

export enum RequestStatus {
  SUCCESS = 0, ERROR = 1
}

export interface AccountListState {
  accounts? : Account[],
  status? : RequestStatus,
  errorMessage? : string
}
