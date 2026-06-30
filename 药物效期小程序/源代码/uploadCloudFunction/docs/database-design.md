# Database Design

This document describes the backend data model for the medicine inventory mini program. It covers cloud database collections, cloud storage paths, indexes, and the cloud function API surface.

## Goals

- Store each user's medicine inventory in cloud database instead of relying only on local storage.
- Support efficient "expiry countdown" sorting by indexing a stable timestamp field.
- Keep AI multimodal inputs in cloud storage and persist only file IDs plus normalized recognition results.
- Keep all database writes behind cloud functions so user ownership and schema normalization are enforced in one place.

## Collections

### `drugs`

One document represents one medicine inventory item owned by one WeChat user.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `_openid` | string | yes | Owner openid injected by the cloud function. |
| `name` | string | yes | Medicine name. |
| `nameSortKey` | string | yes | Lowercase name used for deterministic sorting. |
| `qty` | string | yes | Human-readable quantity, for example `1 bottle` or `6 tablets`. |
| `category` | string | yes | One of `antipyretic`, `antibiotic`, `digestive`, `vitamin`, `external`, `other`. |
| `dosageForm` | string | yes | One of `bottle`, `blister`, `pouch`, `tube`, `other`. |
| `openState` | string | yes | One of `unopened`, `opened`, `independent`. |
| `location` | string | yes | Storage location key, default `medicine_box`. |
| `expireDateText` | string | no | Expiry date in `YYYY-MM-DD`. Empty means unknown. |
| `expireDate` | Date/null | no | Parsed expiry date at local day start. |
| `expireAtTs` | number | yes | Expiry timestamp in milliseconds. Unknown dates use `Number.MAX_SAFE_INTEGER`. |
| `daysToExpire` | number/null | no | Snapshot of days remaining when the record was last written or refreshed. |
| `expiryStatus` | string | yes | One of `expired`, `expiring`, `normal`, `unknown`. |
| `source` | string | yes | One of `manual`, `vision`, `voice`, `migration`, `unknown`. |
| `ai` | object | yes | Recognition metadata: `confidence`, `note`, `rawText`, `model`. |
| `attachments` | object | yes | `{ images: string[], audios: string[] }` cloud file IDs. |
| `searchText` | string | yes | Simple search field composed from name, qty, location, category. |
| `isDeleted` | boolean | yes | Soft delete flag. |
| `createdAt` | Date | yes | Creation time. |
| `createdAtTs` | number | yes | Creation timestamp. |
| `updatedAt` | Date | yes | Last update time. |
| `updatedAtTs` | number | yes | Last update timestamp. |
| `deletedAt` | Date | no | Soft delete time. |
| `deletedAtTs` | number | no | Soft delete timestamp. |

## Indexes

Create these indexes in the cloud database console after creating the `drugs` collection.

| Index name | Fields | Purpose |
| --- | --- | --- |
| `owner_expiry_countdown` | `_openid` asc, `isDeleted` asc, `expireAtTs` asc, `createdAtTs` desc | Main inventory list sorted by expiry countdown. |
| `owner_status_expiry` | `_openid` asc, `isDeleted` asc, `expiryStatus` asc, `expireAtTs` asc | Fast expired/expiring filtering. |
| `owner_category_expiry` | `_openid` asc, `isDeleted` asc, `category` asc, `expireAtTs` asc | Category pages and dashboards. |
| `owner_updated` | `_openid` asc, `isDeleted` asc, `updatedAtTs` desc | Recent changes and synchronization. |

The countdown order should use `expireAtTs` rather than `daysToExpire`. `daysToExpire` changes every day, while `expireAtTs` is stable and index-friendly. The backend refresh action can update `daysToExpire` and `expiryStatus` snapshots for display.

## Cloud Storage

Recommended paths:

| Path | Content | Lifecycle |
| --- | --- | --- |
| `ai-inputs/images/{openid}/{timestamp}-{random}.jpg` | Images uploaded for vision recognition. | Keep while attached to a drug record; delete with `deleteCloudFiles` when no longer needed. |
| `ai-inputs/audio/{openid}/{timestamp}-{random}.mp3` | Voice recordings uploaded for ASR. | Same as images. |

The existing front end currently uploads to `ai-inputs/images/...` and `ai-inputs/audio/...`. When adding user-scoped paths, include `openid` in the cloud path so file ownership checks can be stricter.

## Cloud Functions

### `aiAssistant`

Existing multimodal AI relay. It accepts image or audio cloud file IDs, downloads or exchanges them for temporary URLs, calls the AI model API, and returns normalized candidate medicine items.

Actions:

- `visionRecognize`: `{ fileID }`
- `voiceRecognize`: `{ fileID, fallbackText? }`
- `parseDrugText`: `{ text }`
- `chat`: `{ question, drugs, messages }`

### `database`

Inventory and storage management API. All responses follow:

```json
{ "success": true, "data": {} }
```

Errors follow:

```json
{ "success": false, "code": "INVALID_ARGUMENT", "message": "name is required." }
```

Actions:

| Action | Payload | Description |
| --- | --- | --- |
| `listDrugs` | `{ page?, pageSize?, filter?, sort? }` | Lists current user's non-deleted drugs. Default sort is expiry countdown. |
| `getDrug` | `{ id }` | Gets one owned drug. |
| `createDrug` | `{ drug }` | Creates one normalized drug record. |
| `batchCreateDrugs` | `{ drugs }` or `{ items }` | Creates up to 20 records, useful after AI recognition. |
| `updateDrug` | `{ id, drug }` | Updates one owned record and recalculates expiry fields. |
| `deleteDrug` | `{ id, deleteFiles? }` | Soft deletes a record. Optional file deletion removes attached storage files. |
| `getFileTempUrls` | `{ fileIDs }` | Returns temporary URLs for attached storage files. |
| `deleteCloudFiles` | `{ fileIDs }` | Deletes owned or AI-input cloud files. |
| `refreshExpirySnapshots` | `{ limit? }` | Recalculates `daysToExpire` and `expiryStatus` for up to 50 owned records. |

## Example: Create From AI Recognition

```js
await wx.cloud.callFunction({
  name: "database",
  data: {
    action: "batchCreateDrugs",
    drugs: recognizedItems.map(item => ({
      ...item,
      source: "vision",
      expireDateText: item.date,
      attachments: { images: [fileID], audios: [] }
    }))
  }
});
```

## Operational Notes

- The `database` function no longer writes quickstart test data into `goods`.
- `expireAtTs` is the primary field for "expiry countdown" sorting.
- Run `refreshExpirySnapshots` on app launch, daily login, or a scheduled trigger if the cloud environment supports timers.
- AI output should always be treated as a draft. The front end should let users confirm name, quantity, category, and expiry date before calling `createDrug` or `batchCreateDrugs`.
