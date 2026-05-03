# Hub Domain README

## 1. Muc tieu
Tai lieu nay mo ta domain Hub cho backend/frontend, bao gom:
- Flow tao Hub.
- Flow cap nhat Hub (chi field co ban).
- Flow nhap kho (stock-in) cho Staff/Admin.
- Rule xu ly item category khi nhap kho.

## 2. Dinh nghia
- Hub: Tram trung chuyen cuu tro.
- Item Category: Danh muc vat pham trong bang `item_categories`.
- Hub Inventory: Ton kho cua Hub trong bang `hub_inventories`.

## 3. Quyen truy cap
- Admin:
  - Tao Hub.
  - Cap nhat field co ban cua Hub.
  - Nhap kho Hub.
- Staff:
  - Nhap kho Hub.

## 4. API hien tai (Hub)
- `GET /api/hubs`
  - Public.
  - Liet ke Hub.
- `GET /api/hubs/{id}`
  - Public.
  - Lay chi tiet Hub, kem inventory.
- `POST /api/hubs`
  - Role: ADMIN.
  - Tao Hub.
- `PATCH /api/hubs/{id}`
  - Role: ADMIN.
  - Cap nhat field co ban cua Hub (khong nhap kho o endpoint nay).
- `POST /api/hubs/{id}/inventory/import`
  - Role: STAFF, ADMIN.
  - Nhap kho/bo sung inventory cho Hub.

## 5. Flow tao Hub
### 5.1 Input
Admin tao cac field co ban:
- `name`
- `address`
- `phoneNumber`
- `imageUrl`
- `status`
- `operatingHours`
- `lat`, `lng`

Co the gui them danh sach item ma Hub quan ly qua `elements`:
- `itemCategoryId`
- `quantity`
- `lowStockThreshold` (optional)

### 5.2 Rule xu ly
1. Validate Hub input.
2. Tao Hub.
3. Neu co `elements`:
   - Moi `itemCategoryId` phai ton tai trong `item_categories`.
   - Neu hop le, insert vao `hub_inventories`.
4. Tra ve Hub DTO.

### 5.3 Body mau
```json
{
  "name": "Hub Quan 1",
  "address": "123 Nguyen Hue",
  "phoneNumber": "+84901234567",
  "status": "ACTIVE",
  "operatingHours": "08:00-20:00",
  "lat": 10.7769,
  "lng": 106.7009,
  "elements": [
    {
      "itemCategoryId": "11111111-1111-1111-1111-111111111111",
      "quantity": 100,
      "lowStockThreshold": 20
    }
  ]
}
```

## 6. Flow cap nhat Hub (field co ban)
### 6.1 Input
Chi cap nhat field co ban cua Hub:
- `name`, `address`, `phoneNumber`, `imageUrl`, `status`, `operatingHours`, `lat`, `lng`

### 6.2 Rule quyen
- Chi ADMIN duoc cap nhat.

### 6.3 Rule xu ly
1. Tim Hub theo `id`.
2. Patch cac field duoc gui len.
3. Save Hub.
4. Khong xu ly ton kho tai endpoint nay.

## 7. Flow nhap kho (Stock-in)
Endpoint: `POST /api/hubs/{id}/inventory/import`

### 7.1 Input
- `elements`: danh sach item nhap kho.
- Moi element:
  - `itemCategoryId`
  - `quantity`
  - `lowStockThreshold` (optional)

### 7.2 Rule quyen
- STAFF va ADMIN deu duoc nhap kho.

### 7.3 Rule nghiep vu
1. Tim Hub theo `id`, khong co thi bao loi.
2. Duyet tung item:
   - Neu item da co trong `hub_inventories`:
     - Cong them so luong (stock-in).
     - Cap nhat `lowStockThreshold` neu co.
   - Neu item chua co trong `hub_inventories` nhung co trong `item_categories`:
     - Tao moi dong inventory cho Hub.
   - Neu item khong co trong `item_categories`:
     - Khong cho nhap truc tiep.
     - Staff/Admin phai tao category moi trong danh muc truoc,
       sau do nhap kho lai voi `itemCategoryId` moi.

### 7.4 Body mau
```json
{
  "elements": [
    {
      "itemCategoryId": "11111111-1111-1111-1111-111111111111",
      "quantity": 25,
      "lowStockThreshold": 15
    },
    {
      "itemCategoryId": "22222222-2222-2222-2222-222222222222",
      "quantity": 10
    }
  ]
}
```

## 8. Rule dong bo frontend
Frontend can follow logic:
1. Form tao Hub:
   - Cho admin chon item category tu danh sach category co san (`item_categories`).
2. Form cap nhat Hub:
   - Chi cho sua field co ban, khong gui inventory.
3. Form nhap kho:
   - Cho Staff/Admin chon category tu danh sach co san.
   - Neu chua co category can nhap, dieu huong sang man hinh tao category, sau do quay lai nhap kho.

## 9. Loi thuong gap va xu ly
- Category khong ton tai:
  - Tra loi 404/400 voi message ro rang.
- Hub khong ton tai:
  - Tra loi 404.
- Quantity am:
  - Tu choi validate request.

## 10. Huong phat trien tiep
- Bo sung endpoint category management cho Staff/Admin (neu chua co).
- Bo sung inventory logs cho stock-in de audit (ai nhap, nhap luc nao, so luong truoc/sau).
- Bo sung test integration cho flow stock-in va role authorization.
