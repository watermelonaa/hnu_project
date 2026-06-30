export type DrugCategory =
  | "antipyretic"
  | "antibiotic"
  | "digestive"
  | "vitamin"
  | "external"
  | "other";

export type DosageForm = "bottle" | "blister" | "pouch" | "tube" | "other";

export type OpenState = "unopened" | "opened" | "independent";

export type ExpiryStatus = "expired" | "expiring" | "normal" | "unknown";

export type DrugSource = "manual" | "vision" | "voice" | "migration" | "unknown";

export interface DrugAiMeta {
  confidence: number;
  note: string;
  rawText: string;
  model: string;
}

export interface DrugAttachments {
  images: string[];
  audios: string[];
}

export interface DrugRecord {
  id: string;
  name: string;
  qty: string;
  category: DrugCategory;
  dosageForm: DosageForm;
  openState: OpenState;
  location: string;
  expireDate: string;
  date: string;
  days: number | null;
  status: ExpiryStatus;
  source: DrugSource;
  ai: DrugAiMeta;
  attachments: DrugAttachments;
  createdAt: number;
  updatedAt: number;
}

export interface DrugInput {
  name: string;
  qty: string;
  category?: DrugCategory;
  dosageForm?: DosageForm;
  openState?: OpenState | string;
  location?: string;
  expireDateText?: string;
  expireDate?: string;
  date?: string;
  source?: DrugSource;
  confidence?: number;
  note?: string;
  rawText?: string;
  model?: string;
  ai?: Partial<DrugAiMeta>;
  attachments?: Partial<DrugAttachments>;
}

export interface ListDrugsRequest {
  action: "listDrugs";
  page?: number;
  pageSize?: number;
  filter?: {
    category?: DrugCategory;
    openState?: OpenState;
    expiryStatus?: ExpiryStatus;
    location?: string;
    keyword?: string;
  };
  sort?:
    | "expiryCountdown"
    | "expireAt"
    | "createdAt"
    | "updatedAt"
    | "name"
    | {
        key?: "expiryCountdown" | "expireAt" | "createdAt" | "updatedAt" | "name";
        field?: "expiryCountdown" | "expireAt" | "createdAt" | "updatedAt" | "name";
        direction?: "asc" | "desc";
      };
}

export interface GetDrugRequest {
  action: "getDrug";
  id: string;
}

export interface CreateDrugRequest {
  action: "createDrug";
  drug: DrugInput;
}

export interface BatchCreateDrugsRequest {
  action: "batchCreateDrugs";
  drugs?: DrugInput[];
  items?: DrugInput[];
}

export interface UpdateDrugRequest {
  action: "updateDrug";
  id: string;
  drug: Partial<DrugInput>;
}

export interface DeleteDrugRequest {
  action: "deleteDrug";
  id: string;
  deleteFiles?: boolean;
}

export interface GetFileTempUrlsRequest {
  action: "getFileTempUrls";
  fileIDs: string[];
}

export interface DeleteCloudFilesRequest {
  action: "deleteCloudFiles";
  fileIDs: string[];
}

export interface RefreshExpirySnapshotsRequest {
  action: "refreshExpirySnapshots";
  limit?: number;
}

export type DatabaseRequest =
  | ListDrugsRequest
  | GetDrugRequest
  | CreateDrugRequest
  | BatchCreateDrugsRequest
  | UpdateDrugRequest
  | DeleteDrugRequest
  | GetFileTempUrlsRequest
  | DeleteCloudFilesRequest
  | RefreshExpirySnapshotsRequest;

export interface ListDrugsResponse {
  page: number;
  pageSize: number;
  total: number;
  items: DrugRecord[];
}

export type DatabaseSuccess<T> = {
  success: true;
  data: T;
};

export type DatabaseFailure = {
  success: false;
  code: string;
  message: string;
};

export type DatabaseResponse<T> = DatabaseSuccess<T> | DatabaseFailure;
