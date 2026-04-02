# API Attachment Documentation

## Upload Attachment Endpoint

### Request

**Endpoint:** `POST /attachments`

**Content-Type:** `multipart/form-data`

**Authentication:** Required (Bearer Token)

### Request Parameters

| Parameter        | Type          | Required | Description                                                   |
| ---------------- | ------------- | -------- | ------------------------------------------------------------- |
| `file`           | MultipartFile | **Yes**  | File ảnh/tài liệu cần upload                                  |
| `reference_type` | String        | No       | Loại tham chiếu (ví dụ: "SOS_REQUEST", "MISSION", "DONATION") |
| `reference_id`   | UUID          | No       | ID của đối tượng tham chiếu                                   |

### Request Example

#### cURL

```bash
curl -X POST http://localhost:8080/attachments \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg" \
  -F "reference_type=SOS_REQUEST" \
  -F "reference_id=123e4567-e89b-12d3-a456-426614174000"
```

#### Postman

1. Method: **POST**
2. URL: `http://localhost:8080/attachments`
3. Headers:
   - `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body: **form-data**
   - `file`: [Select File]
   - `reference_type`: `SOS_REQUEST` (text)
   - `reference_id`: `123e4567-e89b-12d3-a456-426614174000` (text)

#### JavaScript (Fetch API)

```javascript
const formData = new FormData();
formData.append("file", fileInput.files[0]);
formData.append("reference_type", "SOS_REQUEST");
formData.append("reference_id", "123e4567-e89b-12d3-a456-426614174000");

const response = await fetch("http://localhost:8080/attachments", {
  method: "POST",
  headers: {
    Authorization: "Bearer YOUR_JWT_TOKEN",
  },
  body: formData,
});

const result = await response.json();
console.log(result);
```

#### Android (Java + Retrofit)

```java
// 1. Define Retrofit Service
public interface AttachmentApi {
    @Multipart
    @POST("attachments")
    Call<AttachmentResponse> uploadAttachment(
        @Part MultipartBody.Part file,
        @Part("reference_type") RequestBody referenceType,
        @Part("reference_id") RequestBody referenceId
    );
}

// 2. Upload File
File imageFile = new File("/path/to/image.jpg");
RequestBody requestFile = RequestBody.create(
    MediaType.parse("image/*"),
    imageFile
);
MultipartBody.Part filePart = MultipartBody.Part.createFormData(
    "file",
    imageFile.getName(),
    requestFile
);

RequestBody refType = RequestBody.create(
    MediaType.parse("text/plain"),
    "SOS_REQUEST"
);
RequestBody refId = RequestBody.create(
    MediaType.parse("text/plain"),
    "123e4567-e89b-12d3-a456-426614174000"
);

Call<AttachmentResponse> call = api.uploadAttachment(filePart, refType, refId);
call.enqueue(new Callback<AttachmentResponse>() {
    @Override
    public void onResponse(Call<AttachmentResponse> call, Response<AttachmentResponse> response) {
        if (response.isSuccessful()) {
            AttachmentResponse result = response.body();
            String imageUrl = result.getData().getUrl();
            // Use imageUrl
        }
    }

    @Override
    public void onFailure(Call<AttachmentResponse> call, Throwable t) {
        // Handle error
    }
});
```

### Response

#### Success Response (201 Created)

```json
{
  "success": true,
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "url": "https://res.cloudinary.com/your-cloud/image/upload/v1234567890/folder/filename.jpg",
    "file_name": "image.jpg",
    "file_size": 245678,
    "mime_type": "image/jpeg",
    "uploaded_by": "user-uuid-here",
    "created_at": "2026-04-02T12:30:00Z"
  }
}
```

#### Error Responses

**401 Unauthorized** - No authentication token

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

**400 Bad Request** - Invalid file or missing required fields

```json
{
  "success": false,
  "message": "Invalid file format or file too large"
}
```

**500 Internal Server Error** - Upload failed

```json
{
  "success": false,
  "message": "Failed to upload attachment"
}
```

## Use Cases

### 1. Upload ảnh SOS Request

```bash
curl -X POST http://localhost:8080/attachments \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@accident.jpg" \
  -F "reference_type=SOS_REQUEST" \
  -F "reference_id=sos-uuid"
```

### 2. Upload ảnh xác nhận Mission

```bash
curl -X POST http://localhost:8080/attachments \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@confirmation.jpg" \
  -F "reference_type=MISSION" \
  -F "reference_id=mission-uuid"
```

### 3. Upload ảnh Donation

```bash
curl -X POST http://localhost:8080/attachments \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@donation-items.jpg" \
  -F "reference_type=DONATION" \
  -F "reference_id=donation-uuid"
```

### 4. Upload avatar (không có reference)

```bash
curl -X POST http://localhost:8080/attachments \
  -H "Authorization: Bearer TOKEN" \
  -F "file=@avatar.jpg"
```

## Notes

1. **File Size Limit**: Tùy thuộc vào cấu hình Cloudinary và Spring Boot (mặc định: 10MB)
2. **Supported Formats**: Image files (JPEG, PNG, GIF, WebP, etc.)
3. **Storage**: Files are stored on Cloudinary CDN
4. **Authentication**: JWT token required in Authorization header
5. **Reference Fields**: Optional, used to link attachment to specific entities (SOS, Mission, Donation, etc.)

## Response Fields Explanation

| Field         | Description                                            |
| ------------- | ------------------------------------------------------ |
| `id`          | UUID của attachment record trong database              |
| `url`         | Public URL của file trên Cloudinary (dùng để hiển thị) |
| `file_name`   | Tên file gốc                                           |
| `file_size`   | Kích thước file (bytes)                                |
| `mime_type`   | MIME type (image/jpeg, image/png, etc.)                |
| `uploaded_by` | UUID của user đã upload                                |
| `created_at`  | Timestamp khi upload                                   |

## Integration with AidBridge Entities

Attachment có thể liên kết với:

- **SOS Requests** (`reference_type: "SOS_REQUEST"`)
- **Missions** (`reference_type: "MISSION"`)
- **Donations** (`reference_type: "DONATION"`)
- **Aid Requests** (`reference_type: "AID_REQUEST"`)
- **User Avatars** (không cần reference_type/id)
